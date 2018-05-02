/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch Tracker for replies for
 *         a given set of groups
 */
public class RequestTracker {
	private ConcurrentMap<Integer, GroupRequestTracker> tracker;
	private TOMMessage recivedRequest;
	private Request myreply;
	private Map<Vertex, Integer> groups;
	private  byte[] requestsent;
	private long start;
	
	public RequestTracker(Map<Vertex, Integer> groups, Request myreply, byte[] requestsent) {
		this.myreply = myreply;
		tracker = new ConcurrentHashMap<>();
		this.groups = groups;
		this.requestsent = requestsent;
		for (Vertex groupId : groups.keySet()) {

			tracker.put(groupId.getID(), new GroupRequestTracker(groups.get(groupId)));

		}
	}
	
	public void start() {
		this.start = System.nanoTime();
	}
	
	public long getElapsed() {
		return System.nanoTime() - start;
	}
	public  Map<Vertex, Integer> getGroups(){
		return groups;
	}
	public byte[] getRequest() {
		return   requestsent;
	}

	public boolean addReply(TOMMessage req, int senderId) {
		// System.out.println(req.getSender());
		// System.out.println(tracker.get(req.getSender()));
		tracker.get(senderId).addReply(req);
		return checkAll();
	}

	private boolean checkAll() {
		for (Integer groupId : tracker.keySet()) {
			if (!tracker.get(groupId).getMajReached()) {
				return false;
			}
		}
		return true;
	}

	public TOMMessage getRecivedRequest() {
		return recivedRequest;
	}

	public Request getMergedReply() {
		Request tmp;
		for (Integer groupID : tracker.keySet()) {
			tmp = tracker.get(groupID).getMajorityReply();
			if (myreply == null) {
				myreply = tmp;
			} else {
				// myreply.setResult(tmp.getGroupResult(groupID), groupID);
//				System.out.println(myreply);
//				System.out.println(tmp);
//				System.out.println(tmp.getResult());
//				System.out.println();
				myreply.mergeReplies(tmp.getResult());
			}
		}
		return myreply;
	}

}
