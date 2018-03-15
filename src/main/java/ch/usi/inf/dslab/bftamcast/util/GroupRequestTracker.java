package ch.usi.inf.dslab.bftamcast.util;

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 * Tracker for the replies from a single group
 */
public class GroupRequestTracker {
	private ConcurrentSet<Request> replies;
	private int majority;
	private boolean majreached = false;
	private int currentMajority = 0;
	private Request majorityReply;

	private long startTime, endTime;

	public GroupRequestTracker(int majority) {
		this.majority = majority;
		this.replies = new ConcurrentSet<>();
		this.startTime = System.nanoTime();
	}

	public boolean addReply(Request reply) {
		int count = 0;
		for (Request r : replies) {
			if (r.equals(reply)) {
				count++;
			}
		}

		this.replies.add(reply);

		if (count > currentMajority) {
			currentMajority = count;
			majorityReply = reply;
		}
		if (currentMajority >= majority) {
			endTime = System.nanoTime();
			majreached = true;
			return true;
		}
		return false;
	}

	public boolean getMajReached() {
		return majreached;
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

	public Request getMajorityReply() {
		return majorityReply;
	}

	public ConcurrentSet<Request> getRequests() {
		return replies;
	}

	public int getMajNeed() {
		return majority;
	}

}
