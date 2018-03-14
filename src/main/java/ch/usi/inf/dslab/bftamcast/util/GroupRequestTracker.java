package ch.usi.inf.dslab.bftamcast.util;

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import io.netty.util.internal.ConcurrentSet;

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
		// System.out.println();
		// System.out.println();
		for (Request r : replies) {
			// System.out.println();
			// System.out.println(r.toString());
			// System.out.println(reply.toString());
			if (r.equals(reply)) {
				count++;
				// System.out.println("same");
			}
		}

		this.replies.add(reply);

		if (count > currentMajority) {
			currentMajority = count;
			majorityReply = reply;
		}
		if (currentMajority >= majority) {
			// System.out.println("MajReached");
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
