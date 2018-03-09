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
	private Tree overlayTree;
	private int groupId;
	protected transient Lock replyLock;
	protected transient Condition contextSet;
	protected transient ReplicaContext rc;
	protected Request req;
	private Map<Integer, byte[]> table;
	private Map<Integer, RequestTracker> repliesTracker;
	private Map<Integer, Request> processedReplies;
	private SortedMap<Integer, Vector<TOMMessage>> globalReplies;
	private Vertex me;

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
		req = new Request();
	}

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

		req.fromBytes(request.getContent());
		System.out.println("seq #" + req.getSeqNumber());
		System.out.println("sender " + request.getSender());
		System.out.println("called manageReply");

		// already processes and answered request to other replicas, send what has been
		// done
		if (processedReplies.containsKey(req.getSeqNumber())) {
			request.reply.setContent(processedReplies.get(req.getSeqNumber()).toBytes());
			rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);
		}
		// client contacted server directly
		else if (req.getDestination().length == 1) {
			execute(req);
			request.reply.setContent(req.toBytes());
			rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);
		}
		// another group contacted me, majority needed
		else {
			// majority of parent group replicas f+1
			Vertex lca = overlayTree.lca(req.getDestination());
			int n = 0;
			if (groupId != lca.groupId) {
				n = (int) Math.ceil((double) (me.parent.proxy.getViewManager().getCurrentViewN()
						+ me.parent.proxy.getViewManager().getCurrentViewF() + 1) / 2.0);
			}

			// save message
			Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());
			// check if majority of parent contacted me, and request is the same
			// -1 because the request used to compare other is already in msgs
			int count = -1;
			Request r = new Request();
			for (TOMMessage m : msgs) {
				r.fromBytes(m.getContent());
				if (r.equals(req)) {
					count++;
				}
			}

			if (count >= n) {

				int[] destinations = req.getDestination();
				int majNeeded = 0;
				boolean addreq = false;
				List<Vertex> toSend = new ArrayList<>();
				// List<Vertex>
				for (int i = 0; i < destinations.length; i++) {
					// I am a target, compute but wait for majority of other destination to execute
					// the same to asnwer
					if (destinations[i] == groupId) {
						// TODO execute just once after reaching majority
						execute(req);
						System.out.println(req.getValue());
						majNeeded++;
						addreq = true;
					}
					// my child in tree is a destination, forward it
					else if (me.childernIDs.contains(destinations[i])) {
						Vertex v = overlayTree.findVertexById(destinations[i]);

						majNeeded += (int) Math.ceil((double) (v.proxy.getViewManager().getCurrentViewN()
								+ v.proxy.getViewManager().getCurrentViewF() + 1) / 2.0);
						toSend.add(v);
					}
					// destination must be in the path of only one of my childrens
					else {

						for (Vertex v : me.children) {
							if (v.inReach(destinations[i])) {
								if (!toSend.contains(v)) {
									majNeeded += (int) Math.ceil((double) (v.proxy.getViewManager().getCurrentViewN()
											+ v.proxy.getViewManager().getCurrentViewF() + 1) / 2.0);
									toSend.add(v);
								}
								break;// only one path
							}
						}
					}

				}

				// no other destination is in my reach, send reply back
				if (toSend.isEmpty()) {
					// TODO store max msgs size, count answers and track it, make sure to answer to
					// all msgs (even not received yet)
					processedReplies.put(req.getSeqNumber(), req);
					for (TOMMessage msg : msgs) {
						msg.reply.setContent(req.toBytes());
						rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
					}
					globalReplies.remove(req.getSeqNumber());
					return;
				} else {

					// else, tracker for received replies and majority needed
					repliesTracker.put(req.getSeqNumber(), new RequestTracker(majNeeded, request.getSender(), request));
					if (addreq) {
						repliesTracker.get(req.getSeqNumber()).addReply(req);
					}
					for (Vertex v : toSend) {
						v.proxy.invokeAsynchRequest(request.getContent(), this, TOMMessageType.ORDERED_REQUEST);
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

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	protected Vector<TOMMessage> saveReply(TOMMessage reply, int seqNumber) {
		Vector<TOMMessage> messages = globalReplies.computeIfAbsent(seqNumber, k -> new Vector<>());
		messages.add(reply);
		return messages;
	}

	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {
		// TODO check reply signature, authenticity? ie reply.signed()
		System.out.println("reply recieved");
		Request replyReq = new Request();
		replyReq.fromBytes(reply.getContent());
		RequestTracker tracker = repliesTracker.get(replyReq.getSeqNumber());

		if (tracker != null && tracker.addReply(replyReq)) {
			Vector<TOMMessage> msgs = globalReplies.get(replyReq.getSeqNumber());
			System.out.println("finish, sent up req # " + replyReq.getSeqNumber());
			tracker.getRecivedRequest().reply.setContent(replyReq.toBytes());
			processedReplies.put(replyReq.getSeqNumber(), replyReq);
			for (TOMMessage msg : msgs) {
				msg.reply.setContent(replyReq.toBytes());
				rc.getServerCommunicationSystem().send(new int[] { msg.getSender() }, msg.reply);
			}
			globalReplies.remove(replyReq.getSeqNumber());
			repliesTracker.remove(replyReq.getSeqNumber());

		}
	}
}
