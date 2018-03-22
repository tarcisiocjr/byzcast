/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.graph.VertexDirect;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch Tracker for replies for
 *         a given set of groups
 */
public class RequestTrackerDirect {
	private ConcurrentMap<Integer, GroupRequestTracker> tracker;
	private TOMMessage recivedRequest;
	private Request myreply;
	private long startTime, endTime;

	public RequestTrackerDirect(Map<VertexDirect, Integer> groups, Request myreply) {
		this.startTime = System.nanoTime();
		this.myreply = myreply;
		tracker = new ConcurrentHashMap<>();
		for (VertexDirect groupId : groups.keySet()) {

			tracker.put(groupId.getGroupId(), new GroupRequestTracker(groups.get(groupId)));

		}
	}

	public boolean addReply(Request req) {
		if (tracker.containsKey(req.getSender())) {
			tracker.get(req.getSender()).addReply(req);
		}
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
		this.endTime = System.nanoTime();
		Request tmp;
		for (Integer groupID : tracker.keySet()) {
			tmp = tracker.get(groupID).getMajorityReply();
			if (myreply == null) {
				myreply = tmp;
			} else {
				// myreply.setResult(tmp.getGroupResult(groupID), groupID);
				myreply.mergeReplies(tmp.getResult());
			}
		}
		return myreply;
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

}
