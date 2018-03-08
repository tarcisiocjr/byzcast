package ch.usi.inf.dslab.bftamcast.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.shaded.org.jctools.queues.ConcurrentCircularArrayQueue;

public class RequestTracker {
	private ConcurrentSet<Request> replies;
	private int majority;
	private int currentMajority = 0;
	private Request majorityReply;
	private TOMMessage recivedRequest;
	private int answerTo;

	public RequestTracker(int majority, int replier,TOMMessage recivedRequest) {
		this.recivedRequest = recivedRequest;
		this.answerTo = replier;
		this.majority = majority;
		this.replies = new ConcurrentSet<>();
	}

	public boolean addReply(Request reply) {
		int count =0;
		for(Request r : replies) {
			if(r.equals(reply)) {
				count ++;
			}
		}
		
		this.replies.add(reply);
		
		if (count > currentMajority) {
			currentMajority = count;
			majorityReply = reply;
		}
		if(currentMajority >= majority) {
			return true;
		}
		return false;
	}
	
	public Request getMajorityReply() {
		return majorityReply;
	}
	
	public TOMMessage getRecivedRequest() {
		return recivedRequest;
	}
	
	public ConcurrentSet<Request> getRequests() {
		return replies;
	}
	
	public int getReplier() {
		return answerTo;
	}
	
	public int getMajNeed() {
		return majority;
	}

}
