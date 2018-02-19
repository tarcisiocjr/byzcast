package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class ClientThread implements Runnable, ReplyListener {

	final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	final int clientId;
	final int groupId;
	final int numOfGroups;
	final boolean verbose;
	final int runTime;
	final int size;
	final int globalPerc;
	private int seqNumber = 0;
	final Random random;
	final Stats localStats, globalStats;
	final Map<Integer, Integer> repliesCounter;
	ProxyIf proxy;
	final Request replyReq;

	public ClientThread(int clientId, int groupId, String globalConfig, String[] localConfigs, boolean verbose,
			int runTime, int valueSize, int globalPerc, boolean ng) {
		this.clientId = clientId;
		this.groupId = groupId;
		this.numOfGroups = (localConfigs == null || localConfigs.length == 0) ? 1 : localConfigs.length;
		this.verbose = verbose;
		this.runTime = runTime;
		this.size = valueSize;
		this.globalPerc = globalPerc;
		this.random = new Random();
		this.localStats = new Stats();
		this.globalStats = new Stats();
		this.proxy = ng ? new Proxy(clientId + 1000 * groupId, globalConfig, null)
				: new Proxy(clientId + 1000 * groupId, globalConfig, localConfigs);
		this.repliesCounter = new HashMap<>();
		this.replyReq = new Request();
	}

	@Override
	public void run() {
		// setup
		Random r = new Random();
		long startTime = System.nanoTime(), now;
		long elapsed = 0, delta = 0, usLat = startTime;
		Request req = new Request();
		// byte[] response;
		// local and global ids
		int[] all = new int[numOfGroups], local = new int[] { groupId };

		// set groups ids?
		for (int i = 0; i < numOfGroups; i++)
			all[i] = i;

		req.setValue(randomString(size).getBytes());
		while (elapsed / 1e9 < runTime) {
			try {
				req.setDestination(r.nextInt(100) >= globalPerc || numOfGroups == 1 ? local : all);
				req.setKey(r.nextInt(Integer.MAX_VALUE));
				req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
				req.setSeqNumber(seqNumber++);

				AsynchServiceProxy prox = proxy.asyncAtomicMulticast(req, this);
				// TODO ask why do this when recieved majority
				// prox.cleanAsynchRequest(requestId);
				int q = (int) Math.ceil(
						(double) (prox.getViewManager().getCurrentViewN() + prox.getViewManager().getCurrentViewF() + 1)
								/ 2.0);
				repliesCounter.put(seqNumber, q);
				System.out.println("sent   seq#" + seqNumber);

				// stats code
//				now = System.nanoTime();
//				elapsed = (now - startTime);
//
//				if (req.getDestination().length > 1)
//					globalStats.store((now - usLat) / 1000);
//				else
//					localStats.store((now - usLat) / 1000);
//
//				usLat = now;
//				if (verbose && elapsed - delta >= 2 * 1e9) {
//					System.out.println("Client " + clientId + " ops/second:"
//							+ (localStats.getPartialCount() + globalStats.getPartialCount())
//									/ ((float) (elapsed - delta) / 1e9));
//					delta = elapsed;
//				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
//		if (localStats.getCount() > 0) {
//			localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt", 15);
//			System.out.println("LOCAL STATS:" + localStats);
//		}
//
//		if (globalStats.getCount() > 0) {
//			globalStats.persist("globalStats-client-g" + groupId + "-" + clientId + ".txt", 15);
//			System.out.println("\nGLOBAL STATS:" + globalStats);
//		}

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
	public void replyReceived(RequestContext reqCtx, TOMMessage msgReply) {
		// TODO check content for null, do stats
		replyReq.fromBytes(msgReply.getContent());
		Integer seqN = replyReq.getSeqNumber();
		Integer count = repliesCounter.get(seqN);
		if (count != null) {
			count--;

			if (count == 0) {
				repliesCounter.remove(seqN);
				System.out.println("recieved   seq#" + seqN);
				// done process req
				// TODO ask why do this when recieved majority
				// prox.cleanAsynchRequest(requestId);
			} else {
				repliesCounter.put(seqN, count);
			}
		}

	}

	// @Override
	// public void receiveResponse(byte[] response) {
	// this.response = response;
	// try {
	// if (response == null && req.getType() == RequestType.SIZE) {
	// System.err.println("Problem");
	// } else {
	// now = System.nanoTime();
	// elapsed = (now - startTime);
	//
	// if (req.getDestination().length > 1)
	// globalStats.store((now - usLat) / 1000);
	// else
	// localStats.store((now - usLat) / 1000);
	//
	// usLat = now;
	// if (verbose && elapsed - delta >= 2 * 1e9) {
	// System.out.println("Client " + clientId + " ops/second:" +
	// (localStats.getPartialCount() + globalStats.getPartialCount()) / ((float)
	// (elapsed - delta) / 1e9));
	// delta = elapsed;
	// }
	// setRequest(req);
	// if(!stop) {
	// proxy.asyncAtomicMulticast(req, this);
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}
}
