/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class RequestTracker {
	private ConcurrentMap<Integer, GroupRequestTracker> tracker;
	private TOMMessage recivedRequest;
	private int answerTo;

	public RequestTracker(Map<Vertex, Integer> groups, TOMMessage original, int replier, boolean plusOne,
			Request senderComputation) {
		tracker = new ConcurrentHashMap<>();
		answerTo = replier;
		recivedRequest = original;
		for (Vertex groupId : groups.keySet()) {
			if (plusOne) {
				tracker.put(groupId.getGroupId(), new GroupRequestTracker(groups.get(groupId)+1));
				//TODO ask what to do when sender is also destination, have to check answers?
				tracker.get(groupId.getGroupId()).addReply(senderComputation);
			}else {
				tracker.put(groupId.getGroupId(), new GroupRequestTracker(groups.get(groupId)));
			}
		}
	}

	public boolean addReply(Request req) {
		tracker.get(req.getSender()).addReply(req);
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

}
