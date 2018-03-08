package ch.usi.inf.dslab.bftamcast.client;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.Tree;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;

public class ConsoleClient implements ReplyListener{
	final Map<Integer, RequestTracker> repliesTracker =new HashMap<>();
//	private int counter = 0;
//	private int secs = 0;
	long startTime, usLat, delta =0;
	final Request replyReq = new Request();
	private static Scanner scanner;

	public static void main(String[] args) {
		int seqN = 0;
		ConsoleClient c = new ConsoleClient();
		CLIParser p = CLIParser.getClientParser(args);
		Random r = new Random();
		int groupId = p.getGroup();
		int clientId = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
//		String globalConfigPath = p.getGlobalConfig();
//		String[] localConfigPaths = p.getLocalConfigs();
		String treeConfigPath = p.getTreeConfig();
		
		Tree overlayTree = new Tree(treeConfigPath, 5);//UUID.randomUUID().hashCode());
//		int numGroups = localConfigPaths == null ? 1 : localConfigPaths.length;
//		ProxyIf proxy = new Proxy(idClient + 1000 * idGroup, globalConfigPath, localConfigPaths);
		Request req = new Request();
//		int[] dest;
//		byte[] result = null;

		Console console = System.console();
		scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Select an option:");
			System.out.println("1. ADD A KEY/VALUE");
			System.out.println("2. READ A VALUE");
			System.out.println("3. REMOVE AND ENTRY");
			System.out.println("4. GET THE SIZE OF THE MAP (multi-partition)");

			
			int cmd = scanner.nextInt();

			StringTokenizer dt;
			int[] n;
			int index;
			AsynchServiceProxy target;
			req.setMsg(23876);
			seqN++;
			System.out.println(seqN);
			switch (cmd) {
			
			case 1:
				System.out.println("Putting value in the distributed map");
				req.setType(RequestType.PUT);
				req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
				req.setSeqNumber(seqN);
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
				c.repliesTracker.put(seqN, new RequestTracker(((int) Math.ceil((double) (target.getViewManager().getCurrentViewN()
						+ target.getViewManager().getCurrentViewF() + 1) / 2.0)), -1));
			
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
	public void replyReceived(RequestContext context, TOMMessage reply) {
		Request replyReq = new Request();
		replyReq.fromBytes(reply.getContent());
		System.out.println("received");
		System.out.println(reply.getSequence());
		RequestTracker tracker = repliesTracker.get(reply.getSequence());
		System.out.println(tracker);
		if(tracker.addReply(replyReq)) {
			System.out.println("finish, sent up req # "  + replyReq.getSeqNumber());
		}
	}
}
