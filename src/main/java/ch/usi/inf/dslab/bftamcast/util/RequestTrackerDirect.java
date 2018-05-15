/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.VertexDirect;
import ch.usi.inf.dslab.bftamcast.kvs.RequestDirect;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch Tracker for replies for
 *         a given set of groups
 */
public class RequestTrackerDirect {
	private ConcurrentMap<Integer, GroupRequestTrackerDirect> tracker;
	private TOMMessage recivedRequest;
	private RequestDirect myreply;
	private long startTime, endTime;
	private boolean merged = false;

	public RequestTrackerDirect(Map<VertexDirect, Integer> groups, RequestDirect myreply) {
		this.startTime = System.nanoTime();
		this.myreply = myreply;
		tracker = new ConcurrentHashMap<>();
		for (VertexDirect groupId : groups.keySet()) {

			tracker.put(groupId.getGroupId(), new GroupRequestTrackerDirect(groups.get(groupId)));

		}
	}

	public boolean addReply(RequestDirect req) {
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

	public RequestDirect getMergedReply() {
		if (!merged) {
			this.endTime = System.nanoTime();
			RequestDirect tmp;
			for (Integer groupID : tracker.keySet()) {
				tmp = tracker.get(groupID).getMajorityReply();
				System.out.println("merging " + tmp + "   g " + groupID + "   " + tmp.getSender());
				if (myreply == null) {
					myreply = tmp;
				} else {
					// myreply.setResult(tmp.getGroupResult(groupID), groupID);
					myreply.setReplies(tmp.getResult(), tmp.getSender());
				}
			}
		}
		return myreply;
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

}
