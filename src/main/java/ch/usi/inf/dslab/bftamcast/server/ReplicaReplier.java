
package ch.usi.inf.dslab.bftamcast.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.BatchExecutable;
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
public class ReplicaReplier implements Replier, Serializable, ReplyListener, FIFOExecutable {

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
	// pending requests waiting for slot
	private transient Queue<RequestTracker> pendingRequests;
	private List<BatchfromBatchTracker> batchespending = new ArrayList<>();
	private Map<Long, Set<BatchTracker>> batches = new HashMap<>();

	/**
	 * Constructor
	 * 
	 * @param RepID
	 * @param groupID
	 * @param treeConfig
	 */
	public ReplicaReplier(int RepID, int groupID, String treeConfig, int maxOutstanding) {

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

		if (req.getType() == RequestType.BATCH) {

			System.out.println("batch");
			if (processedReplies.get(req.getClient()) != null
					&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
				System.out.println("cached answer send");
				message.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
			}
//			int majReplicasOfSender = me.getParent().getProxy().getViewManager().getCurrentViewF() + 1;
			int majReplicasOfSender = 4;

			// save message
			ConcurrentSet<TOMMessage> msgs = saveRequest(message, req.getSeqNumber(), req.getClient());

			if (msgs.size() >= majReplicasOfSender) {

				System.out.println("maj batch");

				Set<Vertex> toforwardto = new HashSet<>();
				req = GroupRequestTracker.getMajreq(msgs, majReplicasOfSender);
				req.setSender(groupId);

				Request[] reqs = Request.ArrayfromBytes(req.getValue());
				Request[] clones = Request.ArrayfromBytes(req.getValue());
				Request[] reply = new Request[reqs.length];
				Map<Integer, Map<Integer, Integer>> repliesToSet = new HashMap<>();

				int count = 0;
				BatchfromBatchTracker b = new BatchfromBatchTracker();
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
						boolean runned = involved.contains(me);
						if (runned) {
							execute(request);
							involved.remove(me);
						}

						toforwardto.addAll(involved);
						repliesToSet.computeIfAbsent(request.getClient(), k -> new HashMap<>());

						repliesToSet.get(request.getClient()).put(request.getSeqNumber(), i);

						BatchTracker bt = new BatchTracker(msgs, request, involved, clones[i],
								request.getDestIdentifier(), request.getClient(), request.getSeqNumber(), runned, true,
								involved.size(), b);

						batches.computeIfAbsent(request.getDestIdentifier(), k -> new ConcurrentSet<>());
						batches.get(request.getDestIdentifier()).add(bt);

					}

				}
				if (count != 0) {
					System.out.println("reply batch     1");
					b.set(msgs, reply, req.getClient(), req.getSeqNumber(), repliesToSet, count);
					batchespending.add(b);
				} else {
					System.out.println("I am done");
					

					for (TOMMessage msg : msgs) {
						msg.reply.setContent(Request.ArrayToBytes(reply));
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
			// another group contacted me, majority needed
			else {
				// majority of parent group replicas f+1
				// int majReplicasOfSender = 0;
				//
				// // this group is not the lcs, so not contacted directly from client
				// if (groupId != overlayTree.getLca(req.getDestIdentifier()).ID) {
				// majReplicasOfSender =
				// me.getParent().getProxy().getViewManager().getCurrentViewF() + 1;
				// }
				//
				//

				// save message
				ConcurrentSet<TOMMessage> msgs = saveRequest(message, req.getSeqNumber(), req.getClient());
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
				req = GroupRequestTracker.getMajreq(msgs, 1);
				req.setSender(groupId);
				// System.out.println("asdflkhjadsfdka " + req);
				boolean addreq = false;
				// List<Vertex>
				Set<Vertex> involved = overlayTree.getRoute(req.getDestIdentifier(), me);
				if (involved.isEmpty()) {
					return;
					// TODO nothing, none of dests are in reach and you are not in dests
					// should never happend, tocheck
				}
				if (involved.contains(me)) {
					execute(req);
					addreq = true;
					involved.remove(me);
				}

				if (involved.isEmpty()) {
					// should never happend, tocheck if not batchreq and only me means direct call
					// form client, so should trigger req.getDestination().length == 1 above;

					// TODO reply
				} else {

					BatchTracker bt = new BatchTracker(msgs, req, involved, new Request(message.getContent()),
							req.getDestIdentifier(), req.getClient(), req.getSeqNumber(), addreq, false,
							involved.size(), null);
					batches.computeIfAbsent(req.getDestIdentifier(), k -> new ConcurrentSet<>());
					batches.get(req.getDestIdentifier()).add(bt);
					// TODO add bt to Toprocess

				}

				// }

			}

		}
		// TODO call batch if enough msgs

		batch(batches, batchespending);

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

	public void batch(Map<Long, Set<BatchTracker>> batches, List<BatchfromBatchTracker> batchToReplyTo) {
		System.out.println("I am bathcing");
		for (Vertex connection : me.getConnections()) {
			List<Request> tobatch = new ArrayList<>();
			Set<BatchTracker> trackers = new HashSet<>();

			for (long identifier : batches.keySet()) {
				Set<Vertex> sendTo = overlayTree.getRoute(identifier, me);

				if (sendTo.contains(connection)) {
					for (BatchTracker b : batches.get(identifier)) {
						if (!b.finished) {
							tobatch.add(b.toforward);
							trackers.add(b);
						}
					}
				}
			}

			if (!tobatch.isEmpty()) {
				System.out.println("I am bathcing and sending");
				Request[] bb = new Request[tobatch.size()];
				tobatch.toArray(bb);
				Request mainReq = new Request(RequestType.BATCH, -1, Request.ArrayToBytes(bb), new int[] {connection.ID},
						sequencenumber++, me.ID, me.ID, overlayTree.getIdentifier( new int[] {connection.ID}));
				byte[]  r= connection.getProxy().invokeOrdered(mainReq.toBytes());
				System.out.println(r);
				Request reply = new Request(r);

				Request[] replies = Request.ArrayfromBytes(reply.getValue());

				for (Request rep : replies) {
					for (BatchTracker b : batches.get(rep.getDestIdentifier())) {
						if (b.clientID == rep.getClient() && b.seqN == rep.getSeqNumber()) {
							b.handle(rep);
							if (b.finished && !b.batch) {
								for (TOMMessage msg : b.toaswer) {
									msg.reply.setContent(Request.ArrayToBytes(replies));
									rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
								}
							} else if (b.finished) {
								b.tracker.hanlde(b.preprocess);
								if (b.tracker.finished) {
									for (TOMMessage msg : b.tracker.originalBatch) {
										rc.getServerCommunicationSystem().send(new int[] { msg.getSender() },
												msg.reply);
									}

								}
							}
						}
					}
				}
			}
		}

	}

}
