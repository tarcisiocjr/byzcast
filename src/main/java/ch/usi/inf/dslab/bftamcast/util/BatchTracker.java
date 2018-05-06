package ch.usi.inf.dslab.bftamcast.util;

import java.util.Set;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

public class BatchTracker {
	Set<TOMMessage> toaswer;
	Request preprocess;
	Set<Vertex> toforwardto;
	TOMMessage toforward;
	int reqID;
	int senderID;
	int seqN;
	boolean base= false;
	
	
	public BatchTracker(Set<TOMMessage> toaswer, Request preprocess, TOMMessage toforward, int senderID,
			int seqN, Set<Vertex> toforwardto, boolean base) {
		this.toaswer = toaswer;
		this.toforwardto=toforwardto;
		this.preprocess = preprocess;
		this.toforward = toforward;
		this.senderID = senderID;
		this.seqN = seqN;
		this.base = base;
	}
	 

}
