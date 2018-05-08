package ch.usi.inf.dslab.bftamcast.util;

import java.util.Arrays;
import java.util.Set;

import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch Tracker for the replies
 *         from a single group
 */
public class GroupRequestTracker {
	private ConcurrentSet<TOMMessage> replies;
	private int majority;
	private boolean majreached = false;
	private Request majorityReply;

	private long startTime, endTime;

	public GroupRequestTracker(int majority) {
		this.majority = majority;
		this.replies = new ConcurrentSet<>();
		this.startTime = System.nanoTime();
	}

	public boolean addReply(TOMMessage reply) {

		this.replies.add(reply);

		if (replies.size() >= majority) {
			endTime = System.nanoTime();
			majreached = true;
			majorityReply = getMajreq(replies, majority);
			return true;
		}
		return false;
	}

	// public Request getMajReply() {
	//
	// for (Request r : replies) {
	// int count = 0;
	// for (Request r2 : replies) {
	// if (r.equals(r2)) {
	// count++;
	// if (count >= majority) {
	// return r;
	// }
	// }
	// }
	// }
	// return null;
	// }

	public boolean getMajReached() {
		return majreached;
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

	public Request getMajorityReply() {
		return majorityReply;
	}

	public ConcurrentSet<TOMMessage> getRequests() {
		return replies;
	}

	public int getMajNeed() {
		return majority;
	}

	public static Request getMajreq(Set<TOMMessage> msgs, int majority) {
		// System.out.println("msg size " + msgs.size() ) ;
		// System.out.println("maj size " + majority) ;
		byte[][] hashes = new byte[msgs.size()][];
		TOMMessage[] accessableMsgs = new TOMMessage[msgs.size()];
		int i = 0;
		for (TOMMessage msg : msgs) {
			hashes[i] = TOMUtil.computeHash(msg.getContent());
			accessableMsgs[i] = msg;
			i++;
		}
		for (int j = 0; j < hashes.length; j++) {
			int count = 1;
			for (int k = 0; k < hashes.length; k++) {
				if (k != j) {
					if (Arrays.equals(hashes[j], hashes[k])) {
						count++;

					}
				}
				if (count >= 0) {
					//TODO fix for batches, but how????
					return new Request(accessableMsgs[j].getContent());
				}

			}
			System.out.println("count = " + count);
		}
		return null;

	}

}
