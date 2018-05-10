
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
import bftsmart.tom.core.messages.TOMMessageType;
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
public class ReplicaReplier implements Replier, Serializable, ReplyListener, FIFOExecutable {// , BatchExecutable {

	private static final long serialVersionUID = 1L;
	// keep the proxy of all groups and compute lca etc/
	private Tree overlayTree;
	private AtomicInteger sequencenumber, out;
	private int groupId;
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
	// pending requests waiting for slot
	private ConcurrentMap<Long, Set<BatchTracker>> batches;
	// since all batches have me as client and sender just track sequence number.
	private ConcurrentMap<Integer, GroupRequestTracker> batchrepttracker;

	/**
	 * Constructor
	 * 
	 * @param RepID
	 * @param groupID
	 * @param treeConfig
	 */
	public ReplicaReplier(int RepID, int groupID, String treeConfig, int maxOutstanding) {
		sequencenumber = new AtomicInteger(0);
		out = new AtomicInteger(0);
		batchrepttracker = new ConcurrentHashMap<>();
		batches = new ConcurrentHashMap<>();
		last = System.currentTimeMillis();
		this.overlayTree = new Tree(treeConfig, UUID.randomUUID().hashCode());
		this.groupId = groupID;
		System.out.println("max out = " + maxOutstanding);
		me = overlayTree.findVertexById(groupID);
		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		RequestWaiingToReachQuorumSize = new ConcurrentHashMap<>();
		repliesTracker = new ConcurrentHashMap<>();
		processedReplies = new ConcurrentHashMap<>();

		table = new TreeMap<>();
		Timer timer = new Timer();

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				// System.out.println("run timer");
				batch();
			}
		}, 5000, 1000);
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
		System.out.println(req);
		// System.out.println("destIdentifier " + req.getDestIdentifier());

		if (req.getType() == RequestType.BATCH) {

			System.out.println("batch");
			if (processedReplies.get(req.getClient()) != null
					&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
				System.out.println("cached");
				message.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
			}
			int majReplicasOfSender = me.getParent().getProxy().getViewManager().getCurrentViewF() + 1;

			// save message
			ConcurrentSet<TOMMessage> msgs = saveRequest(message, req.getSeqNumber(), req.getClient());

			if (msgs.size() == majReplicasOfSender) {

				System.out.println("maj batch");

				Set<Vertex> toforwardto = new HashSet<>();
				req = GroupRequestTracker.getMajreq(msgs,
						me.getParent().getProxy().getViewManager().getCurrentViewF() + 1);
				req.setSender(groupId);

				Request[] reqs = req.batch;
				System.out.println("BATCH SIZE " + reqs.length);
				Request[] clones = reqs.clone();
				Request[] reply = new Request[reqs.length];
				Map<Integer, Map<Integer, Integer>> repliesToSet = new HashMap<>();

				int count = 0;
				BatchfromBatchTracker b = new BatchfromBatchTracker();
				Set<BatchTracker> tmpbatches = new HashSet<>();
				for (int i = 0; i < reqs.length; i++) {
					Request request = reqs[i];
					Set<Vertex> involved = overlayTree.getRoute(request.getDestIdentifier(), me);

					if (involved.size() == 1 && involved.contains(me)) {
						System.out.println("onlyeme in this batch req, execute");
						execute(request);
						reply[i] = request;
					} else if (!involved.isEmpty()) {
						System.out.println("other as well");
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

						System.out.println("expected counter  = " + tocount);
						BatchTracker bt = new BatchTracker(msgs, request, involved, clones[i],
								request.getDestIdentifier(), request.getClient(), request.getSeqNumber(), runned, true,
								tocount, b);

						batches.computeIfAbsent(request.getDestIdentifier(), k -> new ConcurrentSet<>());
						batches.get(request.getDestIdentifier()).add(bt);
						tmpbatches.add(bt);

					} else {
						System.out.println("something wrong batch");
					}

				}
				if (count != 0) {
					// System.out.println("reply batch 1");
					b.set(msgs, reply, req.getClient(), req.getSeqNumber(), repliesToSet, count, req, tmpbatches);
					for (BatchTracker batchTracker : tmpbatches) {
						batchTracker.ready = true;
					}
				} else {
					processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
					// add processed reply to client replies
					processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);

					System.out.println("all batches were only for me, done, reply");
					for (TOMMessage msg : msgs) {
						req.batch = reply;
						msg.reply.setContent(req.toBytes());
						rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
					}

				}

			}

		} else {

			req.setSender(groupId);

			if (req.getDestination().length == 1) {
				System.out.println("only destination,execute and send");

				execute(req);

				message.reply.setContent(req.toBytes());
				rc.getServerCommunicationSystem().send(new int[] { message.getSender() }, message.reply);
				// create entry for client replies if not already these
				processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
				// add processed reply to client replies
				processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
			}
			// another client contacted me, otherwise would be a batch, if no duplicates no
			else {

				req.setSender(groupId);
				Set<Vertex> involved = overlayTree.getRoute(req.getDestIdentifier(), me);
				if (involved.isEmpty()) {
					return;
				}
				int tocount = involved.size();

				Set<Vertex> involvedtrack = new HashSet<>(involved);

				boolean runned = involved.contains(me);
				if (runned) {

					execute(req);
					tocount--;
					involvedtrack.remove(me);
				}

				System.out.println("expected countttt = " + tocount);
				BatchTracker bt = new BatchTracker(message, req, involved, new Request(message.getContent()),
						req.getDestIdentifier(), req.getClient(), req.getSeqNumber(), runned, false, tocount, null);
				bt.ready = true;
				batches.computeIfAbsent(req.getDestIdentifier(), k -> new ConcurrentSet<>());
				batches.get(req.getDestIdentifier()).add(bt);

			}

		}

		// if (cpunt >= 10) {
		// System.out.println("run count");
		// batch();
		//
		// }
		Timer timer = new Timer();

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				// System.out.println("run timer");
				batch();
			}
		}, 10);

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
		if (reply == null) {

		}

		// unpack request from reply
		Request replyReq = new Request(reply.getContent());
		System.out.println("recieved from " + replyReq.getSender() + "  seq   " + replyReq.getSeqNumber());
		// get the tracker for that request
		if (batchrepttracker.get(replyReq.getSeqNumber()) == null) {
			System.out.println("NULLL");
			return;
		}
		System.out.println("NOT NULL");
		GroupRequestTracker tracker = batchrepttracker.get(replyReq.getSeqNumber());
		// add the reply to tracker and if all involved groups reached their f+1 quota
		if (tracker.addReply(reply)) {
			out.decrementAndGet();

			// get reply with all groups replies
			Request sendReply = tracker.getMajorityReply();
			System.out.println("finished aswers from " + sendReply.getSender());

			Request[] replies = sendReply.batch;
			Set<BatchTracker> toremove = new HashSet<>();

			for (Request rep : replies) {
				for (BatchTracker b : batches.get(rep.getDestIdentifier())) {
					if (b.clientID == rep.getClient() && b.seqN == rep.getSeqNumber() && !b.finished) {
						b.handle(rep);
						if (b.finished && !b.batch) {
							toremove.add(b);
							// create entry for client replies if not already these
							processedReplies.computeIfAbsent(rep.getClient(), k -> new ConcurrentHashMap<>());
							// add processed reply to client replies
							processedReplies.get(rep.getClient()).put(rep.getSeqNumber(), rep);
							System.out.println("not from a batch finish req #" + b.preprocess.getSeqNumber());
							for (TOMMessage msg : b.toaswer) {
								msg.reply.setContent(b.preprocess.toBytes());
								rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
							}
						} else if (b.finished) {

							System.out.println("from a batch finish req# " + b.tracker.seqN);
							b.tracker.hanlde(b.preprocess);
							if (b.tracker.finished) {
								System.out.println(" from a batch finish req #" + b.tracker.og.getSeqNumber());

								toremove.addAll(b.tracker.batches);
								processedReplies.computeIfAbsent(b.tracker.og.getClient(),
										k -> new ConcurrentHashMap<>());
								// add processed reply to client replies
								processedReplies.get(b.tracker.og.getClient()).put(b.tracker.og.getSeqNumber(),
										b.tracker.og);

								for (TOMMessage msg : b.tracker.originalBatch) {
									rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
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

			batchrepttracker.remove(replyReq.getSeqNumber());
		}

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

	public void batch() {
		if (out.get() == 0) {
			for (Vertex connection : me.getConnections()) {
				List<Request> tobatch = new ArrayList<>();

				for (long identifier : batches.keySet()) {
					Set<Vertex> sendTo = overlayTree.getRoute(identifier, me);

					if (sendTo.contains(connection)) {
						for (BatchTracker b : batches.get(identifier)) {
							if (b.ready && !b.finished && !b.sent.contains(connection)) {
								b.sent.add(connection);
								tobatch.add(b.toforward);
							}
						}
					}
				}

				if (!tobatch.isEmpty()) {
					out.incrementAndGet();
					Request[] bb = new Request[tobatch.size()];
					tobatch.toArray(bb);
					Request mainReq = new Request(RequestType.BATCH, -1, null, new int[] { connection.ID },
							sequencenumber.incrementAndGet(), me.ID, me.ID,
							overlayTree.getIdentifier(new int[] { connection.ID }), bb);
					batchrepttracker.put(connection.ID,
							new GroupRequestTracker(connection.getProxy().getViewManager().getCurrentViewF() + 1));
					connection.getProxy().invokeAsynchRequest(mainReq.toBytes(), this, TOMMessageType.ORDERED_REQUEST);

					System.out.println("sending batch # " + mainReq.getSeqNumber() + " to " + connection.ID);
				}
			}
		}

	}

}
