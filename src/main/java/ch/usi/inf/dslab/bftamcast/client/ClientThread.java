package ch.usi.inf.dslab.bftamcast.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.UUID;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;
import ch.usi.inf.dslab.bftamcast.util.Stats;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class ClientThread implements Runnable, ReplyListener {

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
	final Map<Integer, RequestTracker> repliesTracker;
//	private int counter = 0;
//	private int secs = 0;
	long startTime, usLat, delta =0;
	private Tree overlayTree;


	final Request replyReq;

	public ClientThread(int clientId, int groupId, boolean verbose,
			int runTime, int valueSize, int globalPerc, boolean ng, String treeConfigPath) {
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
		this.replyReq = new Request();
		this.overlayTree = new Tree(treeConfigPath,UUID.randomUUID().hashCode());
	}

	@Override
	public void run() {
		// setup
		Random r = new Random();
		startTime = System.nanoTime();
		usLat = startTime;
		long now;
		long elapsed = 0;// delta = 0, usLat = startTime;
		Request req = new Request();
		// byte[] response;
		// local and global ids

//		TimerTask statsTask = new TimerTask() {
//
//			@Override
//			public void run() {
//				secs++;
//				System.out.println((counter / secs) + "req/s");
//			}
//		};

		// And From your main() method or any other method
		Timer timer = new Timer();
//		timer.schedule(statsTask, 0, 1000);

	

		req.setValue(randomString(size).getBytes());
		while (elapsed / 1e9 < runTime) {
			try {
				seqNumber++;
				List<Integer> list = new LinkedList<Integer>(overlayTree.getDestinations());
				Collections.shuffle(list);

				req.setDestination(list.subList(r.nextInt(list.size()), list.size()-1).stream().mapToInt(i->i).toArray());
				req.setKey(r.nextInt(Integer.MAX_VALUE));
				req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
				req.setSeqNumber(seqNumber);

				AsynchServiceProxy prox =  overlayTree.lca(req.getDestination()).proxy;
				prox.invokeAsynchRequest(req.toBytes(), this, TOMMessageType.ORDERED_REQUEST);
				// TODO ask why do this when recieved majority
				// prox.cleanAsynchRequest(requestId);
				int q = (int) Math.ceil(
						(double) (prox.getViewManager().getCurrentViewN() + prox.getViewManager().getCurrentViewF() + 1)
								/ 2.0);
				repliesTracker.put(seqNumber, new RequestTracker(((int) Math.ceil((double) (prox.getViewManager().getCurrentViewN()
						+ prox.getViewManager().getCurrentViewF() + 1) / 2.0)), -1, null));
				// System.out.println("sent seq#" + seqNumber);

				// stats code
				now = System.nanoTime();
				elapsed = (now - startTime);
				//
				// if (req.getDestination().length > 1)
				// globalStats.store((now - usLat) / 1000);
				// else
				// localStats.store((now - usLat) / 1000);
				//
				// usLat = now;
				// if (verbose && elapsed - delta >= 2 * 1e9) {
				// System.out.println("Client " + clientId + " ops/second:"
				// + (localStats.getPartialCount() + globalStats.getPartialCount())
				// / ((float) (elapsed - delta) / 1e9));
				// delta = elapsed;
				// }

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		System.out.println("done");
		timer.cancel();
		if (localStats.getCount() > 0) {
            localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt", 15);
            System.out.println("LOCAL STATS:" + localStats);
        }

        if (globalStats.getCount() > 0) {
            globalStats.persist("globalStats-client-g" + groupId + "-" + clientId + ".txt", 15);
            System.out.println("\nGLOBAL STATS:" + globalStats);
        }
		// if (localStats.getCount() > 0) {
		// localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt",
		// 15);
		// System.out.println("LOCAL STATS:" + localStats);
		// }
		//
		// if (globalStats.getCount() > 0) {
		// globalStats.persist("globalStats-client-g" + groupId + "-" + clientId +
		// ".txt", 15);
		// System.out.println("\nGLOBAL STATS:" + globalStats);
		// }

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

	@Override
public void replyReceived(RequestContext context, TOMMessage reply) {
		
		Request replyReq = new Request();
		replyReq.fromBytes(reply.getContent());
		RequestTracker tracker = repliesTracker.get(replyReq.getSeqNumber());
		if (tracker != null && tracker.addReply(replyReq)) {
			System.out.println("finish, sent up req # " + replyReq.getSeqNumber());
			repliesTracker.remove(replyReq.getSeqNumber());
		}
	}

	

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}
}
