package ch.usi.inf.dslab.bftamcast.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

public class BatchTracker {
	public Set<TOMMessage> toaswer;
	public Set<Vertex> sent = new HashSet<>();
	public List<Request> preprocess;
	public Request majReq;
	public Boolean ready = false;
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
	public List<BatchfromBatchTracker> tracker;

	public BatchTracker(Set<TOMMessage> toaswer, Request preprocess, Set<Vertex> toforwardto, Request toforward,
			long destIdentifies, int clientID, int seqN, boolean base, boolean batch, int expectedmerges, BatchfromBatchTracker tracker) {
		this.toaswer = toaswer;
		this.preprocess = new ArrayList<>();
		this.preprocess.add(preprocess);
		this.toforwardto = toforwardto;
		this.toforward = toforward;
		this.destIdentifies = destIdentifies;
		this.clientID = clientID;
		this.seqN = seqN;
		this.base = base;
		this.batch = batch;
		this.expectedmerges = expectedmerges;
		this.tracker= new ArrayList<>();
		this.tracker.add(tracker);
	}
	
	public BatchTracker(TOMMessage toaswer, Request preprocess, Set<Vertex> toforwardto, Request toforward,
			long destIdentifies, int clientID, int seqN, boolean base, boolean batch, int expectedmerges, BatchfromBatchTracker tracker) {
		this.toaswer = new HashSet<>();
		this.toaswer.add(toaswer);
		this.preprocess = new ArrayList<>();
		this.preprocess.add(preprocess);
		this.toforwardto = toforwardto;
		this.toforward = toforward;
		this.destIdentifies = destIdentifies;
		this.clientID = clientID;
		this.seqN = seqN;
		this.base = base;
		this.batch = batch;
		this.expectedmerges = expectedmerges;
		this.tracker= new ArrayList<>();
		this.tracker.add(tracker);
	}
	

	public void handle(Request rep) {
		System.out.println("handling");
		if (!finished) {
			expectedmergescount++;
			System.out.println("expectedmergescount11 " + expectedmergescount + "   expectedmerges111 " +  expectedmerges);
			if (base) {
				majReq.mergeReplies(rep.getResult());
			} else {
				majReq = rep;
				base = true;
			}

			if (expectedmerges == expectedmergescount) {
				System.out.println("finished");
				finished = true;
			}
		}

	}

}
