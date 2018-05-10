package ch.usi.inf.dslab.bftamcast.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.GroupRequestTracker;
import ch.usi.inf.dslab.bftamcast.util.Stats;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */
public class ClientThread implements Runnable, ReplyListener {
	final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	final int clientId;
	final int groupId;
	final boolean verbose;
	private boolean printed = false;
	final int runTime;
	final int size;
	final int globalPerc;
	private int seqNumber = 0;
	final Random random;
	final Stats localStats, globalStats;
	// final ConcurrentMap<Integer, GroupRequestTracker> repliesTracker;
	final Map<Integer, GroupRequestTracker> repliesTracker;
	long startTime, delta = 0, elapsed = 0;
	private Tree overlayTree;
	private long now;
	private int maxoustanding;
	private AtomicInteger out = new AtomicInteger(0);
	private int[] dests, destinations, local;
	Random r = new Random();

	public ClientThread(int clientId, int groupId, boolean verbose, int runTime, int valueSize, int globalPerc,
			boolean ng, String treeConfigPath, int maxOutstanding) {
		this.clientId = clientId;
		this.groupId = groupId;
		this.verbose = verbose;
		this.runTime = runTime;
		this.size = valueSize;
		this.maxoustanding = maxOutstanding;
		this.globalPerc = globalPerc;
		this.random = new Random();
		this.localStats = new Stats();
		this.globalStats = new Stats();
		// this.repliesTracker = new ConcurrentHashMap<>();
		this.repliesTracker = new HashMap<>();

		this.overlayTree = new Tree(treeConfigPath, UUID.randomUUID().hashCode());
	}

	@Override
	public void run() {
		// setup

		startTime = System.nanoTime();

		Request req;
		dests = new int[overlayTree.getDestinations().size()];
		for (int j = 0; j < dests.length; j++) {
			dests[j] = overlayTree.getDestinations().get(j);
		}

		// List<Integer> list = new LinkedList<Integer>(overlayTree.getDestinations());
		System.out.println("global perc = " + globalPerc);

		byte[] value = randomString(size).getBytes();
		local = new int[] { dests[r.nextInt(dests.length)] };
		long destIdentifier = 0;

		for (int i = 0; i < maxoustanding; i++) {
			System.out.println("send");
			int key = r.nextInt(Integer.MAX_VALUE);
			destinations = (r.nextInt(100) >= globalPerc ? local : dests);
			seqNumber++;
			RequestType type = destinations.length > 1 ? RequestType.SIZE : RequestType.PUT;

			destIdentifier = overlayTree.getIdentifier(destinations);
			Vertex lca = overlayTree.getLca(destIdentifier);

			req = new Request(type, key, value, destinations, seqNumber, clientId, clientId, destIdentifier);
			//

			// System.out.println("increment");

			AsynchServiceProxy prox = lca.getProxy();
			prox.invokeAsynchRequest(req.toBytes(), this, TOMMessageType.ORDERED_REQUEST);
			// TODO needed to cancel requests, but will check later for performance
			// prox.cleanAsynchRequest(requestId); when received reply
			repliesTracker.put(seqNumber, new GroupRequestTracker(prox.getViewManager().getCurrentViewF() + 1));

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
		now = System.nanoTime();
		elapsed = (now - startTime);
		if (reply == null) {
			System.err.println("answer is null");
			return;
		}
		if(elapsed / 1e9 >= runTime) {
			System.out.println("done");
			if (localStats.getCount() > 0 && !printed) {
				localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt", 15);
				System.out.println("LOCAL STATS:" + localStats);
			}

			if (globalStats.getCount() > 0 && !printed) {
				globalStats.persist("globalStats-client-g" + groupId + "-" + clientId + ".txt", 15);
				System.out.println("\nGLOBAL STATS:" + globalStats);
			}
			printed = true;
			return;
		}

		// convert reply to request object
		Request replyReq = new Request(reply.getContent());
		// add it to tracker and check if majority of replies reached
		GroupRequestTracker tracker = repliesTracker.get(replyReq.getSeqNumber());

		if (tracker != null && tracker.addReply(reply)) {
			try {
				if (replyReq.getDestination().length > 1)
					globalStats.store(tracker.getElapsedTime() / 1000000);
				else
					localStats.store(tracker.getElapsedTime() / 1000000);
				// System.out.println((verbose && (elapsed - delta >= 2 * 1e9)));
				// System.out.println(elapsed - delta >= 2 * 1e9);
				if (verbose && (elapsed - delta >= 2 * 1e9)) {
					System.out.println("Client " + clientId + " ops/second:"
							+ (localStats.getPartialCount() + globalStats.getPartialCount())
									/ ((float) (elapsed - delta) / 1e9));
					delta = elapsed;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// remove finished request tracker
			repliesTracker.remove(replyReq.getSeqNumber());

			if (elapsed / 1e9 < runTime) {
//				System.out.println("send");
				int key = r.nextInt(Integer.MAX_VALUE);
				byte[] value = randomString(size).getBytes();
				destinations = (r.nextInt(100) >= globalPerc ? local : dests);
				seqNumber++;
				RequestType type = destinations.length > 1 ? RequestType.SIZE : RequestType.PUT;

				long destIdentifier = overlayTree.getIdentifier(destinations);
				Vertex lca = overlayTree.getLca(destIdentifier);

				Request req = new Request(type, key, value, destinations, seqNumber, clientId, clientId,
						destIdentifier);
				//

				// System.out.println("increment");

				AsynchServiceProxy prox = lca.getProxy();
				prox.invokeAsynchRequest(req.toBytes(), this, TOMMessageType.ORDERED_REQUEST);
				// TODO needed to cancel requests, but will check later for performance
				// prox.cleanAsynchRequest(requestId); when received reply
				repliesTracker.put(seqNumber, new GroupRequestTracker(prox.getViewManager().getCurrentViewF() + 1));
			}

		}
		

	}

	@Override
	public void reset() {
		// TODO reset for reply receiver

	}

}
