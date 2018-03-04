package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.graph.Tree;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.io.Console;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;

public class ConsoleClient implements ReplyListener{
	final Map<Integer, Integer> repliesCounter =new HashMap<>();
	private int counter = 0;
	private int secs = 0;
	long startTime, usLat, delta =0;
	final Request replyReq = new Request();

	public static void main(String[] args) {
		ConsoleClient c = new ConsoleClient();
		CLIParser p = CLIParser.getClientParser(args);
		Random r = new Random();
		int idGroup = p.getGroup();
		int idClient = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
//		String globalConfigPath = p.getGlobalConfig();
//		String[] localConfigPaths = p.getLocalConfigs();
		String treeConfigPath = p.getTreeConfig();
		Tree overlayTree = new Tree(treeConfigPath);
//		int numGroups = localConfigPaths == null ? 1 : localConfigPaths.length;
//		ProxyIf proxy = new Proxy(idClient + 1000 * idGroup, globalConfigPath, localConfigPaths);
		Request req = new Request();
		int[] dest;
		byte[] result = null;

		Console console = System.console();
		Scanner sc = new Scanner(System.in);

		while (true) {
			System.out.println("Select an option:");
			System.out.println("1. ADD A KEY/VALUE");
			System.out.println("2. READ A VALUE");
			System.out.println("3. REMOVE AND ENTRY");
			System.out.println("4. GET THE SIZE OF THE MAP (multi-partition)");

			int cmd = sc.nextInt();

			StringTokenizer dt;
			int[] n;
			int index;
			AsynchServiceProxy target;
			switch (cmd) {
			case 1:
				System.out.println("Putting value in the distributed map");
				req.setType(RequestType.PUT);
				req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				req.setDestination(n);
				req.setValue(console.readLine("Enter the value: ").getBytes());
				System.out.println(n.toString());
				target = overlayTree.lca(n).proxy;
				System.out.println("target aquired");
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				System.out.println("sent");
				// result = proxy.atomicMulticast(req);
//				System.out.println("previous value: " + (result == null ? "NULL" : new String(result)));
				break;
			case 2:
				System.out.println("Reading value from the map");
				req.setType(RequestType.GET);
				req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
				req.setValue(null);
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				req.setDestination(n);
				target = overlayTree.lca(n).proxy;
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				break;
			case 3:
				System.out.println("Removing value in the map");
				req.setType(RequestType.REMOVE);
				req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				req.setDestination(n);
				target = overlayTree.lca(n).proxy;
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				break;
			case 4:
				System.out.println("Getting the map size");
//				req.setType(RequestType.SIZE);
//				req.setKey(0);
//				req.setValue(null);
//				dest = new int[numGroups];
//				for (int i = 0; i < dest.length; i++)
//					dest[i] = i;
//
//				req.setDestination(dest);
//				// result = proxy.atomicMulticast(req);
//				System.out.println("result size = " + result.length);
//				for (int i = 0; i < dest.length; i++)
//					System.out.println("Map size (group " + i + "): " + (result == null ? "NULL"
//							: ByteBuffer.wrap(Arrays.copyOfRange(result, i * 4, i * 4 + 4)).getInt()));

				break;
			default:
				System.err.println("Invalid option...");
			}
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
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
//
//				long now = System.nanoTime();
//				long elapsed = (now - startTime);
//
//				if (replyReq.getDestination().length > 1)
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
				System.out.println("req#" + seqN);
				counter++;
				repliesCounter.remove(seqN);
				// System.out.println("recieved seq#" + seqN);
				// done process req
				// TODO ask why do this when recieved majority
				// prox.cleanAsynchRequest(requestId);
			} else {
				repliesCounter.put(seqN, count);
			}
		}

	}
}
