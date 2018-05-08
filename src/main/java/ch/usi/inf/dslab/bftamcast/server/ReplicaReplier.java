
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.BatchTracker;
import ch.usi.inf.dslab.bftamcast.util.BatchfromBatchTracker;
import ch.usi.inf.dslab.bftamcast.util.GroupRequestTracker;
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;

import io.netty.util.internal.ConcurrentSet;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
// TODO since BatchExecutable calls the same method as FIFOExecutable in
// performance try to remove BatchExecutable to check perf.
// public class ReplicaReplier implements Replier, FIFOExecutable,
// BatchExecutable, Serializable, ReplyListener {
public class ReplicaReplier implements Replier, Serializable, ReplyListener, FIFOExecutable {// ,BatchExecutable{

	private static final long serialVersionUID = 1L;
	// keep the proxy of all groups and compute lca etc/
	private Tree overlayTree;
	private int batchsize = 10;
	private int sequencenumber = 0;
	private List<Request> toprocess = new ArrayList<>();
	private int groupId, maxOutstanding;
	protected transient Lock replyLock;
	protected transient Condition contextSet;
	protected transient ReplicaContext rc;
	protected Request req;
	long last;

	// key store map
	private Map<Integer, byte[]> table;
	// trackers for replies from replicas
	private transient ConcurrentMap<Integer, ConcurrentHashMap<Integer, RequestTracker>> repliesTracker;
	// map for finished requests replies
	private transient ConcurrentMap<Integer, ConcurrentHashMap<Integer, Request>> processedReplies;
	// map for not processed requests
	private transient ConcurrentMap<Integer, ConcurrentHashMap<Integer, ConcurrentSet<TOMMessage>>> RequestWaiingToReachQuorumSize;
	// vertex in the overlay tree representing my group
	private Vertex me;
	private int cpunt = 0;
	// pending requests waiting for slot
	private transient Queue<RequestTracker> pendingRequests;
	private Map<Long, Set<BatchTracker>> batches = new HashMap<>();

	/**
	 * Constructor
	 * 
	 * @param RepID
	 * @param groupID
	 * @param treeConfig
	 */
	public ReplicaReplier(int RepID, int groupID, String treeConfig, int maxOutstanding) {
		last = System.currentTimeMillis();
		this.overlayTree = new Tree(treeConfig, UUID.randomUUID().hashCode());
		this.groupId = groupID;
		this.maxOutstanding = maxOutstanding;
		System.out.println("max out = " + maxOutstanding);
		me = overlayTree.findVertexById(groupID);
		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		RequestWaiingToReachQuorumSize = new ConcurrentHashMap<>();
		repliesTracker = new ConcurrentHashMap<>();
		processedReplies = new ConcurrentHashMap<>();
		pendingRequests = new LinkedBlockingQueue<>();

		table = new TreeMap<>();
	}

	@Override
	public void manageReply(TOMMessage message, MessageContext msgCtx) {
		System.out.println("call manage reply");
		while (rc == null) {
			try {
				this.replyLock.lock();
				this.contextSet.await();
				this.replyLock.unlock();
			} catch (InterruptedException ex) {
				Logger.getLogger(ReplicaReplier.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		// extract request from tom message

		req = new Request(message.getContent());
		System.out.println("destIdentifier "+req.getDestIdentifier());

		System.out.println(req.getSender() + " fdsakjfhjk");
		if (req.getType() == RequestType.BATCH) {

			System.out.println("batch");
			if (processedReplies.get(req.getClient()) != null
					&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
				System.out.println("cached answer send");
				message.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
			}
			 int majReplicasOfSender =
			 4;//me.getParent().getProxy().getViewManager().getCurrentViewF() + 1;

			// save message
			ConcurrentSet<TOMMessage> msgs = saveRequest(message, req.getSeqNumber(), req.getClient());
			System.out.println(req);

			if (msgs.size() == majReplicasOfSender) {

				System.out.println("maj batch");

				Set<Vertex> toforwardto = new HashSet<>();
				req = GroupRequestTracker.getMajreq(msgs,
						me.getParent().getProxy().getViewManager().getCurrentViewF() + 1);
				req.setSender(groupId);

				Request[] reqs = req.batch;
				System.out.println("BATCH SIZE        " + reqs.length);
				Request[] clones = reqs.clone();
				Request[] reply = new Request[reqs.length];
				Map<Integer, Map<Integer, Integer>> repliesToSet = new HashMap<>();

				int count = 0;
				BatchfromBatchTracker b = new BatchfromBatchTracker();
				Set<BatchTracker> tmpbatches = new HashSet<>();
				for (int i = 0; i < reqs.length; i++) {
					Request request = reqs[i];
					Set<Vertex> involved = overlayTree.getRoute(request.getDestIdentifier(), me);
					System.out.println(involved.size());

					if (involved.size() == 1 && involved.contains(me)) {
						System.out.println("onlyemeeee");
						execute(request);
						reply[i] = request;
					} else if (!involved.isEmpty()) {
						count++;
						int tocount = involved.size();

						boolean runned = involved.contains(me);
						if (runned) {

							execute(request);
							tocount--;
						}

						toforwardto.addAll(involved);
						toforwardto.remove(me);
						repliesToSet.computeIfAbsent(request.getClient(), k -> new HashMap<>());

						repliesToSet.get(request.getClient()).put(request.getSeqNumber(), i);

						BatchTracker bt = new BatchTracker(msgs, request, involved, clones[i],
								request.getDestIdentifier(), request.getClient(), request.getSeqNumber(), runned, true,
								tocount, b);

						batches.computeIfAbsent(request.getDestIdentifier(), k -> new ConcurrentSet<>());
						batches.get(request.getDestIdentifier()).add(bt);
						tmpbatches.add(bt);
						cpunt++;

					}

				}
				if (count != 0) {
					System.out.println("reply batch     1");
					b.set(msgs, reply, req.getClient(), req.getSeqNumber(), repliesToSet, count, req, tmpbatches);
				} else {
					System.out.println("I am done");

					for (TOMMessage msg : msgs) {
						req.batch = reply;
						msg.reply.setContent(req.toBytes());
						rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
					}

				}

			}

		} else {

			System.out.println("not batch");

			req.setSender(groupId);

			// already processes and answered request to other replicas, send what has
			// been done
			if (processedReplies.get(req.getClient()) != null
					&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
				System.out.println("cached answer send");
				message.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
			}
			// client contacted server directly, no majority needed
			else if (req.getDestination().length == 1) {
				System.out.println("only destination, send");

				execute(req);

				message.reply.setContent(req.toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
				// create entry for client replies if not already these
				processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
				// add processed reply to client replies
				processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
			}
			// another client contacted me, otherwise would be a batch
			else {
				//already processed
//				if (processedReplies.get(req.getClient()) != null
//						&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
//					System.out.println("cached answer send");
//					message.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
//					rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
//				}
//				// client contacted only me, execute and reply
//				else
					if (req.getDestination().length == 1) {
					System.out.println("only destination, send");

					execute(req);

					message.reply.setContent(req.toBytes());
					rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
					// create entry for client replies if not already these
					processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
					// add processed reply to client replies
					processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
				}
				// another group contacted me, majority needed
				else {

					// save message
//					ConcurrentSet<TOMMessage> msgs = saveRequest(message, req.getSeqNumber(), req.getClient());
					// check if majority of parent contacted me, and request is the same
					// -1 because the request used to compare other is already in msgs

					// majority of replicas sent request and this replica is not already
					// processing
					// the request (not processing it more than once)
					// count
					// if (msgs.size() >= majReplicasOfSender &&
					// (processedReplies.get(req.getClient()) != null
					// && !processedReplies.get(req.getClient()).containsKey(req.getSeqNumber()))) {

					// req = GroupRequestTracker.getMajreq(msgs, majReplicasOfSender);
//					req = GroupRequestTracker.getMajreq(msgs, 1);
					req.setSender(groupId);
					// System.out.println("asdflkhjadsfdka " + req);
					boolean addreq = false;
					// List<Vertex>
					Set<Vertex> involved = overlayTree.getRoute(req.getDestIdentifier(), me);
					System.out.print("ivolved = ");
					for (Vertex vertex : involved) {
						System.out.print(vertex.ID + " ");
					}
					System.out.println();
					if (involved.isEmpty()) {
						return;
						// TODO nothing, none of dests are in reach and you are not in dests
						// should never happend, tocheck
					}
					int c = involved.size();
					Set<Vertex> involvedtrack = new HashSet<>(involved);
					if (involved.contains(me)) {
						execute(req);
						addreq = true;
						involvedtrack.remove(me);
						c--;
					}

					if (involved.isEmpty()) {
						// should never happend, tocheck if not batchreq and only me means direct call
						// form client, so should trigger req.getDestination().length == 1 above;

						// TODO reply
					} else {

						cpunt++;
						BatchTracker bt = new BatchTracker(message, req, involved, new Request(message.getContent()),
								req.getDestIdentifier(), req.getClient(), req.getSeqNumber(), addreq, false, c, null);
						batches.computeIfAbsent(req.getDestIdentifier(), k -> new ConcurrentSet<>());
						batches.get(req.getDestIdentifier()).add(bt);

					}

				}

			}

		}
//		Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//            	System.out.println("run timer");
//    			batch(batches);
//            }
//        }, 50);
//      
    


		

//		if (cpunt >= 10) {
			System.out.println("run count");
			batch(batches);

//		}

	}

	/**
	 * save TomMessage for each client and seq#, to track how many requests have
	 * been received (target f+1 identical)
	 * 
	 * @param request
	 * @param seqNumber
	 * @param clientID
	 * @return the vector of received request for a given client and sequence
	 *         number, used to check f+1
	 */
	protected ConcurrentSet<TOMMessage> saveRequest(TOMMessage request, int seqNumber, int clientID) {
		Map<Integer, ConcurrentSet<TOMMessage>> map = RequestWaiingToReachQuorumSize.computeIfAbsent(clientID,
				k -> new ConcurrentHashMap<>());
		ConcurrentSet<TOMMessage> messages = map.computeIfAbsent(seqNumber, k -> new ConcurrentSet<>());
		if (request != null) {
			messages.add(request);
		}
		return messages;
	}

	/**
	 * execute the request give as parameter and put the resulting byte[] in the
	 * request result[groupid][]
	 * 
	 * @param req
	 */
	protected void execute(Request req) {
		System.out.println("executing");
		byte[] resultBytes;
		switch (req.getType()) {
		case PUT:
			resultBytes = table.put(req.getKey(), req.getValue());
			break;
		case GET:
			resultBytes = table.get(req.getKey());
			break;
		case REMOVE:
			resultBytes = table.remove(req.getKey());
			break;
		case SIZE:
			resultBytes = String.valueOf(table.size()).getBytes();
			break;
		default:
			resultBytes = null;
			System.err.println("Unknown request type: " + req.getType());
		}

		// set result for this group
		req.setResult(resultBytes, groupId);
	}

	/**
	 * extract the replica context
	 */
	@Override
	public void setReplicaContext(ReplicaContext rc) {
		this.replyLock.lock();
		this.rc = rc;
		this.contextSet.signalAll();
		this.replyLock.unlock();
	}

	/**
	 * Async reply reciever
	 */
	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {
		System.out.println("magic");
		// handleMsg(reply, true);
	}

	public Tree getOverlayTree() {
		return overlayTree;
	}

	public Vertex getMyVertex() {
		return me;
	}

	/////// TODO Override methods, maybe to fix, will look into it later

	@Override
	public void reset() {

	}

	@Override
	public byte[] executeOrderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
		// System.out.println("FIFO");
		return bytes;
	}

	@Override
	public byte[] executeUnorderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
		throw new UnsupportedOperationException("Universal replier only accepts ordered messages");
	}

	@Override
	public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
		throw new UnsupportedOperationException("All ordered messages should be FIFO");
	}

	@Override
	public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
		throw new UnsupportedOperationException("All ordered messages should be FIFO");
	}

	public void batch(Map<Long, Set<BatchTracker>> batches) {
		System.out.println("I am bathcing");
		for (Vertex connection : me.getConnections()) {
			List<Request> tobatch = new ArrayList<>();

			for (long identifier : batches.keySet()) {
				Set<Vertex> sendTo = overlayTree.getRoute(identifier, me);
				System.out.print("ivolved = ");
				for (Vertex vertex : sendTo) {
					System.out.print(vertex.ID + " ");
				}
				System.out.println();

				if (sendTo.contains(connection)) {
					for (BatchTracker b : batches.get(identifier)) {
						if (!b.finished && !b.sent.contains(connection)) {
							b.sent.add(connection);
							tobatch.add(b.toforward);
						}
					}
				}
			}

			if (!tobatch.isEmpty()) {
				System.out.println("I am bathcing and sending to " + connection.ID);
				Request[] bb = new Request[tobatch.size()];
				tobatch.toArray(bb);
				Request mainReq = new Request(RequestType.BATCH, -1, null, new int[] { connection.ID },
						sequencenumber++, me.ID, me.ID, overlayTree.getIdentifier(new int[] { connection.ID }), bb);
				// TODO call async and do the rest in callback
				byte[] r = connection.getProxy().invokeOrdered(mainReq.toBytes());
				System.out.println(r);
				//TODO check null
				Request reply = new Request(r);

				Request[] replies = reply.batch;
				Set<BatchTracker> toremove = new HashSet<>();

				for (Request rep : replies) {
					for (BatchTracker b : batches.get(rep.getDestIdentifier())) {
						if (b.clientID == rep.getClient() && b.seqN == rep.getSeqNumber()) {
							b.handle(rep);
							if (b.finished && !b.batch) {
								toremove.add(b);
								// create entry for client replies if not already these
								processedReplies.computeIfAbsent(rep.getClient(), k -> new ConcurrentHashMap<>());
								// add processed reply to client replies
								processedReplies.get(rep.getClient()).put(rep.getSeqNumber(), rep);
								cpunt--;
								System.out.println("not a batch");
								for (TOMMessage msg : b.toaswer) {
									msg.reply.setContent(b.preprocess.toBytes());
									rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
								}
							} else if (b.finished) {
								cpunt--;

								System.out.println("a batch");
								b.tracker.hanlde(b.preprocess);
								if (b.tracker.finished) {
									toremove.addAll(b.tracker.batches);
									processedReplies.computeIfAbsent(b.tracker.og.getClient(),
											k -> new ConcurrentHashMap<>());
									// add processed reply to client replies
									processedReplies.get(b.tracker.og.getClient()).put(b.tracker.og.getSeqNumber(),
											b.tracker.og);

									for (TOMMessage msg : b.tracker.originalBatch) {
										rc.getServerCommunicationSystem().send(new int[] { msg.getSender() },
												msg.reply);
									}

								}
							}
						}
					}
				}
				// delete processed batches
				for (BatchTracker batchTracker : toremove) {
					batches.get(batchTracker.destIdentifies).remove(batchTracker);
				}
			}

		}

	}

	// can not run if have to wait for maj, would always work only for root of tree
	// (always contacted by client only)
//	 @Override
//	 public byte[][] executeBatch(byte[][] command, MessageContext[] msgCtx) {
//	
//	
//	 Request[] reqs = new Request[command.length];
//	
//	 for (int i = 0; i < reqs.length; i++) {
//	 reqs[i] = new Request(command[i]);
//	 }
//	
//	 for (Request r : reqs) {
//	
//	 }
//	 return null;
//	 }

}
