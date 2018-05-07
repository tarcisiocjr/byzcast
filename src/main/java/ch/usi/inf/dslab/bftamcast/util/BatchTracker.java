package ch.usi.inf.dslab.bftamcast.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

public class BatchTracker {
	public Set<TOMMessage> toaswer;
	public Request preprocess;
	public Set<Vertex> toforwardto;
	public Request toforward;
	public long destIdentifies;
	public int clientID;
	public int seqN;
	public boolean base;
	public boolean batch;
	public int expectedmerges;
	public int expectedmergescount = 0;
	public boolean finished = false;
	public BatchfromBatchTracker tracker;

	public BatchTracker(Set<TOMMessage> toaswer, Request preprocess, Set<Vertex> toforwardto, Request toforward,
			long destIdentifies, int clientID, int seqN, boolean base, boolean batch, int expectedmerges, BatchfromBatchTracker tracker) {
		this.toaswer = toaswer;
		this.preprocess = preprocess;
		this.toforwardto = toforwardto;
		this.toforward = toforward;
		this.destIdentifies = destIdentifies;
		this.clientID = clientID;
		this.seqN = seqN;
		this.base = base;
		this.batch = batch;
		this.expectedmerges = expectedmerges;
		this.tracker= tracker;
	}
	

	public void handle(Request rep) {
		if (!finished) {
			expectedmergescount++;
			if (base) {
//				preprocess.mergeReplies(rep.getResult());
			} else {
				preprocess = rep;
				base = true;
			}

			if (expectedmerges == expectedmergescount) {
				finished = true;
			}
		}

	}

}
