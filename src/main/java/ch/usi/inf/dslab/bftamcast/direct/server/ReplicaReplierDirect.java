
package ch.usi.inf.dslab.bftamcast.direct.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import bftsmart.tom.server.BatchExecutable;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.direct.graph.TreeDirect;
import ch.usi.inf.dslab.bftamcast.direct.graph.VertexDirect;
import ch.usi.inf.dslab.bftamcast.direct.kvs.RequestDirect;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class ReplicaReplierDirect implements Replier, FIFOExecutable, BatchExecutable, Serializable, ReplyListener {

	private static final long serialVersionUID = 1L;
	// keep the proxy of all groups and compute lca etc/
	private TreeDirect overlayTree;
	private int groupId, repID;
	protected transient Lock replyLock;
	protected transient Condition contextSet;
	protected transient ReplicaContext rc;
	protected RequestDirect req;

	// key store map
	private Map<Integer, byte[]> table;
	// trackers for replies from replicas
	// private ConcurrentMap<Integer, ConcurrentHashMap<Integer, RequestTracker>>
	// repliesTracker;
	// map for finished requests replies
	private ConcurrentMap<Integer, ConcurrentHashMap<Integer, RequestDirect>> processedReplies;
	// map for not processed requests
	private ConcurrentMap<Integer, ConcurrentHashMap<Integer, ConcurrentSet<TOMMessage>>> globalReplies;
	// vertex in the overlay tree representing my group
	private VertexDirect me;

	/**
	 * Constructor
	 * 
	 * @param RepID
	 * @param groupID
	 * @param treeConfig
	 */
	public ReplicaReplierDirect(int repID, int groupID, String treeConfig) {

		this.overlayTree = new TreeDirect(treeConfig, UUID.randomUUID().hashCode(), null);
		this.groupId = groupID;
		this.repID = repID;
		me = overlayTree.findVertexById(groupID);
		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		globalReplies = new ConcurrentHashMap<>();
		// repliesTracker = new ConcurrentHashMap<>();
		processedReplies = new ConcurrentHashMap<>();

		table = new TreeMap<>();
	}

	@Override
	public void manageReply(TOMMessage request, MessageContext msgCtx) {

		if (repID == 2) {

			while (rc == null) {
				try {
					this.replyLock.lock();
					this.contextSet.await();
					this.replyLock.unlock();
				} catch (InterruptedException ex) {
					Logger.getLogger(ReplicaReplierDirect.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			req = new RequestDirect(request.getContent());
			req.setSender(groupId);
			// hack to establish connections
			// if (req.getType() == RequestType.NOP) {
			// System.out.println("connected " + msgCtx.getSender());
			// rc.getServerCommunicationSystem().send(new int[] { req.getClient() },
			// request.reply);
			// return;
			// }

			System.out.println("new message");

			// already processes and answered request to other replicas, send what has
			// been done
			if (processedReplies.get(req.getClient()) != null
					&& processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
				System.out.println("already processed, reply directly, req# " + req.getSeqNumber());
				for (int i1 = 0; i1 < 4; i1++) {
					rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);

				}

			}
			// client contacted server directly, no majority needed
			else if (req.getDestination().length == 1) {
				System.out.println("only destination, server contacted me directly, exec and reply directly");
				execute(req);

				request.reply.setContent(req.toBytes());
				// rc.getServerCommunicationSystem().send(new int[] { request.getSender() },
				// request.reply);
				System.out.println("seeending  req# " + req.getSeqNumber() + "client " + req.getClient());
				for (int i1 = 0; i1 < 4; i1++) {

					rc.getServerCommunicationSystem().send(new int[] { req.getClient() }, request.reply);
				}
				// create entry for client replies if not already these
				processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
				// add processed reply to client replies
				processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
			}
			// another group contacted me, majority needed
			else {
				System.out.println("else");
				// majority of parent group replicas f+1
				VertexDirect lca = overlayTree.lca(req.getDestination());
				for (int i : req.getDestination()) {
					System.out.println("sdfsdf    " + i);
				}
				System.out.println("fasdf");
				System.out.println(lca);
				System.out.println(lca.getGroupId());
				System.out.println(groupId);
				int majReplicasOfSender = 0;
				// this group is not the lcs, so not contacted directly from client
				if (groupId != lca.getGroupId()) {
					System.out.println("not contacted directly from client");
					majReplicasOfSender = me.getParent().getProxy().getViewManager().getCurrentViewF() + 1;
				}

				// save message
				ConcurrentSet<TOMMessage> msgs = saveRequest(request, req.getSeqNumber(), req.getClient());
				// check if majority of parent contacted me, and request is the same
				// -1 because the request used to compare other is already in msgs
				int count = -1;
				RequestDirect r;
				for (TOMMessage m : msgs) {
					r = new RequestDirect(m.getContent());
					if (r.equals(req)) {
						count++;
					}
				}
				System.out.println("count = " + count);
				System.out.println("maj = " + majReplicasOfSender);

				// majority of replicas sent request and this replica is not already
				// processing
				// the request (not processing it more than once)
				if (count >= majReplicasOfSender && (!processedReplies.containsKey(req.getClient())
						|| !processedReplies.get(req.getClient()).containsKey(req.getSeqNumber()))) {
					System.out.println("processing!!! req " + req.getSeqNumber());

					globalReplies.get(req.getClient()).remove(req.getSeqNumber());
					//
					int[] destinations = req.getDestination();
					Map<VertexDirect, Integer> toSend = new HashMap<>();
					// List<Vertex>
					for (int i = 0; i < destinations.length; i++) {
						// I am a target, compute but wait for majority of other destination to
						// execute
						// the same to asnwer
						if (destinations[i] == groupId) {
							execute(req);
							// System.out.println(req.getValue());
							// addreq = true;
							System.out.println("majority reached, I am a destination, execute and send!");
							processedReplies.computeIfAbsent(req.getClient(), k -> new ConcurrentHashMap<>());
							// add processed reply to client replies
							processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);

							// can remove, later requests will receive answers directly from already
							// processes replies
							request.reply.setContent(req.toBytes());

							System.out.println("seeending  req# " + req.getSeqNumber() + "client " + req.getClient());
							for (int i1 = 0; i1 < 4; i1++) {
								rc.getServerCommunicationSystem().send(new int[] { req.getClient() }, request.reply);
							}
							globalReplies.get(req.getClient()).remove(req.getSeqNumber());
						}
						// my child in tree is a destination, forward it
						else if (me.getChildernIDs().contains(destinations[i])) {
							VertexDirect v = overlayTree.findVertexById(destinations[i]);
							toSend.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
						}
						// destination must be in the path of only one of my childrens
						else {

							for (VertexDirect v : me.getChildren()) {
								if (v.inReach(destinations[i])) {
									if (!toSend.keySet().contains(v)) {
										toSend.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
									}
									break;// only one path
								}
							}
						}

					}

					for (VertexDirect v : toSend.keySet()) {
						// System.out.println("sending to OTHERS req " + req.getSeqNumber());
						//
						// v.getProxy().invokeAsynchRequest(req.toBytes(), this,
						// TOMMessageType.ORDERED_REQUEST);
						for (int i = 0; i < 4; i++) {
							v.getProxy().invokeAsynchRequest(req.toBytes(), this, TOMMessageType.ORDERED_REQUEST);

						}
					}
				}
			}
		}
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
		Map<Integer, ConcurrentSet<TOMMessage>> map = globalReplies.computeIfAbsent(clientID,
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
	protected void execute(RequestDirect req) {
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
		System.out.println("async magic???");
	}

	public TreeDirect getOverlayTree() {
		return overlayTree;
	}

	public VertexDirect getVertex() {
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

	@Override
	public byte[][] executeBatch(byte[][] command, MessageContext[] msgCtx) {
		// System.out.println("BATCH");

		return command;
	}
}
