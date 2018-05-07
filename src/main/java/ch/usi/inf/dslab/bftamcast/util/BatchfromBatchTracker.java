package ch.usi.inf.dslab.bftamcast.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

public class BatchfromBatchTracker {

	public Set<TOMMessage> originalBatch;
	public Request[] replies;
	public int clientID;
	public int seqN;
	public int expectedUpdates;
	public int expectedUpdatescount = 0;
	public boolean finished = false;
	public Map<Integer, Map<Integer, Integer>> repliesToSet = new HashMap<>();

	// clientid seqnum index of replies to set
	public BatchfromBatchTracker() {
	};

	public BatchfromBatchTracker(Set<TOMMessage> originalBatch, Request[] replies, int clientID, int seqN,
			Map<Integer, Map<Integer, Integer>> repliesToSet, int expectedUpdates) {
		super();
		this.originalBatch = originalBatch;
		this.replies = replies;
		this.clientID = clientID;
		this.seqN = seqN;
		this.repliesToSet = repliesToSet;
		this.expectedUpdates = expectedUpdates;
	}

	public void set(Set<TOMMessage> originalBatch, Request[] replies, int clientID, int seqN,
			Map<Integer, Map<Integer, Integer>> repliesToSet, int expectedUpdates) {
		this.originalBatch = originalBatch;
		this.replies = replies;
		this.clientID = clientID;
		this.seqN = seqN;
		this.repliesToSet = repliesToSet;
		this.expectedUpdates = expectedUpdates;
	}

	public void hanlde(Request preprocess) {
		if (!finished) {
			expectedUpdatescount++;
			int i = repliesToSet.get(preprocess.getClient()).get(preprocess.getSeqNumber());
			Request base = replies[i];
			if (base == null) {
				replies[i] = preprocess;
			} else {
//				base.mergeReplies(preprocess.getResult());
			}

			finished = expectedUpdates == expectedUpdatescount;
			if (finished) {
				for (TOMMessage msg : originalBatch) {
					msg.reply.setContent(Request.ArrayToBytes(replies));
				}
			}
		}

	}

}
