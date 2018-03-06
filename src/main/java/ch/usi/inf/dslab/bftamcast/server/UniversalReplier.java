/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 *         Temporary class to start experimenting and understanding byzcast
 * 
 *         - understand byzcast structure - make server and clients non blocking
 *         (asynchronous) - remove auxiliary groups and use target groups to
 *         build the overlay tree
 */
public class UniversalReplier implements Replier, FIFOExecutable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Tree overlayTree;
	private String confTree;
	private int groupID;
	protected transient Lock replyLock;
	protected transient Condition contextSet;
	protected transient ReplicaContext rc;
	protected Request req;
	private Map<Integer, byte[]> table;
	private SortedMap<Integer, Vector<TOMMessage>> globalReplies;

	public UniversalReplier(int groupID, String treeConfig) {
		this.confTree = treeConfig;
		this.overlayTree = new Tree(treeConfig);
		this.groupID = groupID;

		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		globalReplies = new TreeMap<>();
		table = new TreeMap<>();
		req = new Request();
	}

	@Override
	public void manageReply(TOMMessage request, MessageContext msgCtx) {
		// call second
		while (rc == null) {
			try {
				this.replyLock.lock();
				this.contextSet.await();
				this.replyLock.unlock();
			} catch (InterruptedException ex) {
				Logger.getLogger(AmcastLocalReplier.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		req.fromBytes(request.reply.getContent());
		System.out.println("seq #" + req.getSeqNumber());
		System.out.println("seq #" + req.getMsg());
		System.out.println("sender" + request.getSender() + " " + request.reply.acceptSentTime);
		System.out.println("called manageReply");
		req = new Request();
		System.out.println("test seq #" + req.getMsg());
		req.fromBytes(request.getContent());
		System.out.println("seq #" + req.getSeqNumber());
		System.out.println("seq #" + req.getMsg());
		System.out.println("sender" + request.getSender() + " " + request.reply.acceptSentTime);
		System.out.println("called manageReply");
		Boolean furtherDests = false;
		int index = -1;
		for (int i = 0; i < req.getDestination().length; i++) {
			if (req.getDestination()[i] == groupID) {
				index = i;
				// execute
			} else {
				furtherDests = true;
			}
		}
		req.setMsg(groupID);
		request.reply.setContent(req.toBytes());
		System.out.println(groupID);
		rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);

		if (furtherDests) {
			// testing
			
			for (int i = 0; i < req.getDestination().length; i++) {
				if(req.getDestination()[i] != groupID) {
//					rc.getServerCommunicationSystem().getClientsConn().
//					overlayTree.findVertexById(req.getDestination()[i]).asyncAtomicMulticast(req, msgCtx.); //find replyserver from somewhere
				}
			}

			// check if children,check reach of childrens
			

//			for (Vertex v : overlayTree.findVertexById(groupID).children()) {
//				for
				
				// check if it's a destination and if further destinations are reachable
			}
			// compute new destination list for each child to send the msg

			// store in req??

			int[] dests;
			if (index != -1) {

			} else {
				dests = req.getDestination();
			}
			// overlayTree.lca(furtherDests.toArray(int));
		} else {
			// send reply?? or send anyway and wait for anwers from all groups
		}

	}

	protected Request execute(Request req) {
		byte[] resultBytes;
		boolean toMe = false;

		for (int i = 0; i < req.getDestination().length; i++) {
			if (req.getDestination()[i] == groupID) {
				toMe = true;
				break;
			}
		}

		if (!toMe) {
			// System.out.println("Message not addressed to my group.");
			req.setType(RequestType.NOP);
			req.setValue(null);
		} else {
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
		return req;
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
		System.out.println("called executeOrderedFIFO");
		// call first
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

	private boolean contains(int[] array, int item) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == item) {
				return true;
			}
		}
		return false;
	}

}
