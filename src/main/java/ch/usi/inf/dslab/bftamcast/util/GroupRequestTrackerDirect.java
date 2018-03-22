package ch.usi.inf.dslab.bftamcast.util;

import ch.usi.inf.dslab.bftamcast.kvs.RequestDirect;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 * Tracker for the replies from a single group
 */
public class GroupRequestTrackerDirect {
	private ConcurrentSet<RequestDirect> replies;
	private int majority;
	private boolean majreached = false;
	private int currentMajority = 0;
	private RequestDirect majorityReply;

	private long startTime, endTime;

	public GroupRequestTrackerDirect(int majority) {
		this.majority = majority;
		this.replies = new ConcurrentSet<>();
		this.startTime = System.nanoTime();
	}

	public boolean addReply(RequestDirect reply) {
		int count = 0;
		for (RequestDirect r : replies) {
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

	public RequestDirect getMajorityReply() {
		return majorityReply;
	}

	public ConcurrentSet<RequestDirect> getRequests() {
		return replies;
	}

	public int getMajNeed() {
		return majority;
	}

}
