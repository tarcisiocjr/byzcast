/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;
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
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 *         Temporary class to start experimenting and understanding byzcast
 * 
 *         - understand byzcast structure - make server and clients non blocking
 *         (asynchronous) - remove auxiliary groups and use target groups to
 *         build the overlay tree
 */
public class ReplicaReplier implements Replier, FIFOExecutable, Serializable, ReplyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//keep the proxy of all groups and comput lca etc/
	private Tree overlayTree;
	private int groupId;
	protected transient Lock replyLock;
	protected transient Condition contextSet;
	protected transient ReplicaContext rc;
	
	//request container, put received message here
	protected Request req;
	
	//keystore map
	private Map<Integer, byte[]> table;
	//trackers for replies from replicas
	private Map<Integer, Map<Integer, RequestTracker>> repliesTracker;
	//map for finished requests replies
	private Map<Integer, Map<Integer, Request>> processedReplies;
	//map for not processed requests
	private Map<Integer, Map<Integer, Vector<TOMMessage>>> globalReplies;
	//vertex in the overlay tree representing my group
	private Vertex me;

	/**
	 * Constructor
	 * @param RepID
	 * @param groupID
	 * @param treeConfig
	 */
	public ReplicaReplier(int RepID, int groupID, String treeConfig) {

		this.overlayTree = new Tree(treeConfig, UUID.randomUUID().hashCode());
		this.groupId = groupID;
		me = overlayTree.findVertexById(groupID);

		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		globalReplies = new TreeMap<>();
		repliesTracker = new HashMap<>();
		processedReplies = new HashMap<>();

		table = new TreeMap<>();
	}

	/**
	 * Called every time a message is received
	 */
	@Override
	public void manageReply(TOMMessage request, MessageContext msgCtx) {
		// TODO check reply signature, authenticity? ie reply.signed()
		while (rc == null) {
			try {
				this.replyLock.lock();
				this.contextSet.await();
				this.replyLock.unlock();
			} catch (InterruptedException ex) {
				Logger.getLogger(ReplicaReplier.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		//extract request from tom message
		req = new Request(request.getContent());


		// already processes and answered request to other replicas, send what has been
		// done
		if (processedReplies.get(req.getClient()) != null && processedReplies.get(req.getClient()).containsKey(req.getSeqNumber())) {
			request.reply.setContent(processedReplies.get(req.getClient()).get(req.getSeqNumber()).toBytes());
			rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);
		}
		// client contacted server directly, no majority needed
		else if (req.getDestination().length == 1) {
			execute(req);
			request.reply.setContent(req.toBytes());
			rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);
			//create entry for client replies if not already these
			processedReplies.computeIfAbsent(req.getClient(), k -> new HashMap<>());
			//add processed reply to client replies
			processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
		}
		// another group contacted me, majority needed
		else {
			// majority of parent group replicas f+1
			Vertex lca = overlayTree.lca(req.getDestination());
			int majReplicasOfSender = 0;
			//this group is not the lcs, so not contacted directly from client
			if (groupId != lca.getGroupId()) {
				majReplicasOfSender = (int) Math.ceil((double) (me.parent.getProxy().getViewManager().getCurrentViewN()
						+ me.parent.getProxy().getViewManager().getCurrentViewF() + 1) / 2.0);
			}

			// save message
			Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber(), req.getClient());
			// check if majority of parent contacted me, and request is the same
			// -1 because the request used to compare other is already in msgs
			int count = -1;
			Request r;
			for (TOMMessage m : msgs) {
				r= new Request(m.getContent());
				if (r.equals(req)) {
					count++;
				}
			}

			// majority of replicas sent request and this replica is not already processing the request (not processing it more than once)
			if (count >= majReplicasOfSender && (repliesTracker.get(req.getClient()) == null || !repliesTracker.get(req.getClient()).containsKey(req.getSeqNumber()))) {

				int[] destinations = req.getDestination();
				int majNeeded = 0;
				boolean addreq = false;
				List<Vertex> toSend = new ArrayList<>();
				// List<Vertex>
				for (int i = 0; i < destinations.length; i++) {
					// I am a target, compute but wait for majority of other destination to execute
					// the same to asnwer
					if (destinations[i] == groupId) {
						execute(req);
						System.out.println(req.getValue());
						majNeeded++;
						addreq = true;
					}
					// my child in tree is a destination, forward it
					else if (me.getChildernIDs().contains(destinations[i])) {
						Vertex v = overlayTree.findVertexById(destinations[i]);

						majNeeded += (int) Math.ceil((double) (v.getProxy().getViewManager().getCurrentViewN()
								+ v.getProxy().getViewManager().getCurrentViewF() + 1) / 2.0);
						toSend.add(v);
					}
					// destination must be in the path of only one of my childrens
					else {

						for (Vertex v : me.getChildren()) {
							if (v.inReach(destinations[i])) {
								if (!toSend.contains(v)) {
									majNeeded += (int) Math.ceil((double) (v.getProxy().getViewManager().getCurrentViewN()
											+ v.getProxy().getViewManager().getCurrentViewF() + 1) / 2.0);
									toSend.add(v);
								}
								break;// only one path
							}
						}
					}

				}

				// no other destination is in my reach, send reply back
				if (toSend.isEmpty()) {		
					//create entry for client replies if not already these
					processedReplies.computeIfAbsent(req.getClient(), k -> new HashMap<>());
					//add processed reply to client replies
					processedReplies.get(req.getClient()).put(req.getSeqNumber(), req);
					for (TOMMessage msg : msgs) {
						msg.reply.setContent(req.toBytes());
						rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
					}
					//can remove, later requests will receive answers directly from already processes replies
					globalReplies.get(req.getClient()).remove(req.getSeqNumber());
					return;
				} else {

					// else, tracker for received replies and majority needed
					//add map for a client tracker if absent
					repliesTracker.computeIfAbsent(req.getClient(), k -> new HashMap<>());
					repliesTracker.get(req.getClient()).put(req.getSeqNumber(), new RequestTracker(majNeeded, request.getSender(), request));
					if (addreq) {
						repliesTracker.get(req.getClient()).get(req.getSeqNumber()).addReply(req);
					}
					for (Vertex v : toSend) {
						v.getProxy().invokeAsynchRequest(request.getContent(), this, TOMMessageType.ORDERED_REQUEST);
					}
				}
			}
		}

	}

	protected void execute(Request req) {
		System.out.println("executed");
		byte[] resultBytes;
		System.out.println(req.getType().toString());
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
			resultBytes = ByteBuffer.allocate(4).putInt(table.size()).array();
			break;
		default:
			resultBytes = null;
			System.err.println("Unknown request type: " + req.getType());
		}

		req.setValue(resultBytes);
		}

	@Override
	public void setReplicaContext(ReplicaContext rc) {
		this.replyLock.lock();
		this.rc = rc;
		this.contextSet.signalAll();
		this.replyLock.unlock();
	}

	@Override
	public byte[] executeOrderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
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

	

	protected Vector<TOMMessage> saveReply(TOMMessage reply, int seqNumber, int clientID) {
		Map<Integer, Vector<TOMMessage>> map = globalReplies.computeIfAbsent(clientID, k -> new HashMap<>());
		Vector<TOMMessage> messages = map.computeIfAbsent(seqNumber, k -> new Vector<>());
		messages.add(reply);
		return messages;
	}

	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {
		// TODO check reply signature, authenticity? ie reply.signed()
		System.out.println("reply recieved");
		Request replyReq = new Request(reply.getContent());
		RequestTracker tracker = repliesTracker.get(replyReq.getClient()).get(replyReq.getSeqNumber());

		if (tracker != null && tracker.addReply(replyReq)) {
			Vector<TOMMessage> msgs = globalReplies.get(replyReq.getClient()).get(replyReq.getSeqNumber());
			System.out.println("finish, sent up req # " + replyReq.getSeqNumber());
			tracker.getRecivedRequest().reply.setContent(replyReq.toBytes());
			processedReplies.computeIfAbsent(replyReq.getClient(), k -> new HashMap<>());
			processedReplies.get(replyReq.getClient()).put(replyReq.getSeqNumber(), replyReq);
			for (TOMMessage msg : msgs) {
				msg.reply.setContent(replyReq.toBytes());
				rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
			}
			globalReplies.get(req.getClient()).remove(replyReq.getSeqNumber());
			repliesTracker.get(req.getClient()).remove(replyReq.getSeqNumber());

		}
	}
	
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}
}
