package ch.usi.inf.dslab.bftamcast.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.TreeDirect;
import ch.usi.inf.dslab.bftamcast.graph.VertexDirect;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.GroupRequestTracker;
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;
import ch.usi.inf.dslab.bftamcast.util.RequestTrackerDirect;
import ch.usi.inf.dslab.bftamcast.util.Stats;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */
public class ClientThreadDirect implements Runnable, ReplyListener {
	final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	final int clientId;
	final int groupId;
	final boolean verbose;
	final int runTime;
	final int size;
	final int globalPerc;
	private int seqNumber = 0;
	final Random random;
	final Stats localStats, globalStats;
	final Map<Integer, GroupRequestTracker> repliesTracker;
	final Map<Integer, RequestTrackerDirect> repliesTracker2;
	long startTime, delta = 0, elapsed = 0;
	private TreeDirect overlayTree;
	private ReentrantLock lock = new ReentrantLock();

	public ClientThreadDirect(int clientId, int groupId, boolean verbose, int runTime, int valueSize, int globalPerc,
			boolean ng, String treeConfigPath) {
		this.clientId = clientId;
		this.groupId = groupId;
		this.verbose = verbose;
		this.runTime = runTime;
		this.size = valueSize;
		this.globalPerc = globalPerc;
		this.random = new Random();
		this.localStats = new Stats();
		this.globalStats = new Stats();
		this.repliesTracker = new HashMap<>();
		this.repliesTracker2 = new HashMap<>();
		this.overlayTree = new TreeDirect(treeConfigPath, UUID.randomUUID().hashCode(), null);
	}

	@Override
	public void run() {
		// setup
		Random r = new Random();
		startTime = System.nanoTime();

		Request req;
		double perc = globalPerc / 100;
		int[] dests = new int[overlayTree.getDestinations().size()];
		for (int j = 0; j < dests.length; j++) {
			dests[j] = overlayTree.getDestinations().get(j);
		}

		List<Integer> list = new LinkedList<Integer>(overlayTree.getDestinations());

		while (elapsed / 1e9 < runTime) {
			try {

				seqNumber++;
				byte[] value = randomString(size).getBytes();
				int[] destinations;
				int key = r.nextInt(Integer.MAX_VALUE);

				Collections.shuffle(list);
				if (r.nextDouble() <= perc) {
					destinations = dests;
				} else {
					destinations = new int[] { dests[r.nextInt(dests.length)] };
				}
				RequestType type = destinations.length > 1 ? RequestType.SIZE : RequestType.PUT;

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId);

				AsynchServiceProxy prox = overlayTree.lca(req.getDestination()).getProxy();
				prox.invokeAsynchRequest(req.toBytes(), this, TOMMessageType.ORDERED_REQUEST);
				// TODO maybe needed to cancel requests, but will check later for performance
				// prox.cleanAsynchRequest(requestId);
//				repliesTracker.put(seqNumber, new GroupRequestTracker(prox.getViewManager().getCurrentViewF() + 1));
				Map<VertexDirect, Integer> totrack = new HashMap<>();
				VertexDirect v;
				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		lock.lock();
		try {
			System.out.println("done");
			if (localStats.getCount() > 0) {
				localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt", 15);
				System.out.println("LOCAL STATS:" + localStats);
			}

			if (globalStats.getCount() > 0) {
				globalStats.persist("globalStats-client-g" + groupId + "-" + clientId + ".txt", 15);
				System.out.println("\nGLOBAL STATS:" + globalStats);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 
	 * @param len
	 * @return return a string of size len of random characters from the char[]
	 *         symbols
	 */
	String randomString(int len) {
		char[] buf = new char[len];

		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}

	/**
	 * Async reply reciever
	 */
	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {
		if (reply == null) {
			System.err.println("answer is null");
			return;
		}
		// convert reply to request object
		Request replyReq = new Request(reply.getContent());
		// add it to tracker and check if majority of replies reached
		// GroupRequestTracker tracker = repliesTracker.get(replyReq.getSeqNumber());
		//
		// if (tracker != null && tracker.addReply(replyReq)) {
		// lock.lock();
		// try {
		// if (replyReq.getDestination().length > 1)
		// globalStats.store(tracker.getElapsedTime() / 1000);
		// else
		// localStats.store(tracker.getElapsedTime() / 1000);
		// if (verbose && elapsed - delta >= 2 * 1e9) {
		// System.out.println("Client " + clientId + " ops/second:"
		// + (localStats.getPartialCount() + globalStats.getPartialCount())
		// / ((float) (elapsed - delta) / 1e9));
		// delta = elapsed;
		// }
		// }finally {
		// lock.unlock();
		// }
		//
		// //remove finished request tracker
		// repliesTracker.remove(replyReq.getSeqNumber());
		// }
		// get the tracker for that request
		RequestTrackerDirect tracker = repliesTracker2.get(replyReq.getSeqNumber());
		// add the reply to tracker and if all involved groups reached their f+1 quota
		if (tracker != null && tracker.addReply(replyReq)) {
			// get reply with all groups replies
			Request finalReply = tracker.getMergedReply();
			try {
				if (finalReply.getDestination().length > 1)
					globalStats.store(tracker.getElapsedTime() / 1000);
				else
					localStats.store(tracker.getElapsedTime() / 1000);
				if (verbose && elapsed - delta >= 2 * 1e9) {
					System.out.println("Client " + clientId + " ops/second:"
							+ (localStats.getPartialCount() + globalStats.getPartialCount())
									/ ((float) (elapsed - delta) / 1e9));
					delta = elapsed;
				}
			} finally {
				lock.unlock();
			}

			// remove finished request tracker
			repliesTracker2.remove(replyReq.getSeqNumber());

		}
	

	}

	

	@Override
	public void reset() {
		// TODO reset for reply receiver

	}
}