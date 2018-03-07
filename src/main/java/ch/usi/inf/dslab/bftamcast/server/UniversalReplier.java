/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
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
public class UniversalReplier implements Replier, FIFOExecutable, Serializable, ReplyListener {

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
	private SortedMap<Integer, Vector<TOMMessage>> globalReplies;
	private Vertex me;

	public UniversalReplier(int RepID, int groupID, String treeConfig) {
		
		this.overlayTree = new Tree(treeConfig,UUID.randomUUID().hashCode());
		this.groupId = groupID;
		me = overlayTree.findVertexById(groupID);

		replyLock = new ReentrantLock();
		contextSet = replyLock.newCondition();
		globalReplies = new TreeMap<>();
		table = new TreeMap<>();
		req = new Request();
	}

	@Override
	public void manageReply(TOMMessage request, MessageContext msgCtx) {
		//http://www.javapractices.com/topic/TopicAction.do?Id=56
//		UUID.fromString("server"+group+serverid)
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

		
		req.fromBytes(request.getContent());
		System.out.println("seq #" + req.getSeqNumber());
		System.out.println("seq #" + req.getMsg());
		System.out.println("sender " + request.getSender());
		System.out.println("called manageReply");
		
		
//		for
//		if me execute
//		if in child send
//		else for child, if child reach send, if not already sent (child also a destination)
		boolean sent = false;
		int[] destinations = req.getDestination();
		for (int i = 0; i < destinations.length; i++) {
			if(destinations[i] == groupId) {
				//execute
//				Request ans = execute(req);
				//reply
				request.reply.setContent(req.toBytes());
//				rc.getServerCommunicationSystem().send(new int[] { request.getSender() }, request.reply);
				rc.getServerCommunicationSystem().send(new int[] {5 }, request.reply);
			}
			//my child in tree is a destination, forward it
			else if(me.childernIDs.contains(destinations[i])){
				overlayTree.findVertexById(destinations[i]).asyncAtomicMulticast(req, this);
			}
			//destination must be in the path of only one of my childrens (tree), have to do it just once.
			else if (!sent) {
				sent = true;
				
				for(Vertex v : me.children) {
					if(v.inReach(destinations[i])) {
						v.asyncAtomicMulticast(req, this);
						break;
					}
				}
			}
			
			
		}
		
	
	

	}

	protected Request execute(Request req) {
		byte[] resultBytes;
		boolean toMe = false;

		for (int i = 0; i < req.getDestination().length; i++) {
			if (req.getDestination()[i] == groupId) {
				toMe = true;
				break;
			}
		}

		if (!toMe) {
			 System.out.println("Message not addressed to my group.");
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

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {
		System.out.println("received");
		// TODO Auto-generated method stub
		
	}

}
