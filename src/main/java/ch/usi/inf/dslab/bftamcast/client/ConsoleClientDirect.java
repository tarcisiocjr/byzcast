package ch.usi.inf.dslab.bftamcast.client;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.TreeDirect;
import ch.usi.inf.dslab.bftamcast.graph.VertexDirect;
import ch.usi.inf.dslab.bftamcast.kvs.RequestDirect;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;
import ch.usi.inf.dslab.bftamcast.util.RequestTrackerDirect;

public class ConsoleClientDirect implements ReplyListener {
	private static Scanner scanner;
	final Map<Integer, RequestTrackerDirect> repliesTracker2 = new HashMap<>();
	final Semaphore lock =  new Semaphore(1);

	public static void main(String[] args) {
		int seqNumber = 0;
		Map<VertexDirect, Integer> totrack;
		ConsoleClientDirect c = new ConsoleClientDirect();
		CLIParser p = CLIParser.getClientParser(args);
		int clientId = UUID.randomUUID().hashCode();
		String treeConfigPath = p.getTreeConfig();
		TreeDirect overlayTree = new TreeDirect(treeConfigPath, clientId, c);
		RequestDirect req;

		for (Integer i : overlayTree.getDestinations()) {
			VertexDirect v = overlayTree.findVertexById(i);
			totrack = new HashMap<>();
			totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
			c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));

			v.getProxy().invokeAsynchRequest(
					new RequestDirect(RequestType.NOP, -1, new byte[0], new int[] {v.getGroupId()}, seqNumber, clientId, clientId).toBytes(), c,
					TOMMessageType.ORDERED_REQUEST);
			seqNumber++;
		}

		Console console = System.console();
		scanner = new Scanner(System.in);
		while (true) {

			System.out.println("Client: " + clientId);
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
			seqNumber++;
			byte[] value;
			int[] destinations;
			int key;
			RequestType type;
			VertexDirect v;
			

			switch (cmd) {

			case 1:
				System.out.println("Putting value in the distributed map");
				type = RequestType.PUT;
				key = Integer.parseInt(console.readLine("Enter the key: "));
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				destinations = n;
				totrack = new HashMap<>();

				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				value = console.readLine("Enter the value: ").getBytes();

				req = new RequestDirect(type, key, value, destinations, seqNumber, clientId, clientId);

				target = overlayTree.lca(n).getProxy();
				totrack = new HashMap<>();
				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				System.out.println("sent");
				break;
			case 2:
				System.out.println("Reading value from the map");
				type = RequestType.GET;
				key = Integer.parseInt(console.readLine("Enter the key: "));
				value = null;
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				destinations = n;
				totrack = new HashMap<>();
				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				target = overlayTree.lca(n).getProxy();
				req = new RequestDirect(type, key, value, destinations, seqNumber, clientId, clientId);

				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				break;
			case 3:
				System.out.println("Removing value in the map");
				type = RequestType.REMOVE;
				key = Integer.parseInt(console.readLine("Enter the key: "));
				value = null;
				dt = new StringTokenizer(console.readLine("Enter the destinations: "), " ");
				n = new int[dt.countTokens()];
				index = 0;
				while (dt.hasMoreElements()) {
					n[index] = Integer.parseInt(dt.nextToken());
					index++;
				}
				destinations = n;
				totrack = new HashMap<>();
				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				target = overlayTree.lca(n).getProxy();
				req = new RequestDirect(type, key, value, destinations, seqNumber, clientId, clientId);
				// c.repliesTracker.put(seqNumber, new
				// GroupRequestTracker(target.getViewManager().getCurrentViewF() + 1));
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				break;
			case 4:
				System.out.println("Getting the map size");
				type = RequestType.SIZE;
				key = 0;
				value = null;

				destinations = new int[overlayTree.getDestinations().size()];
				for (int i = 0; i < destinations.length; i++) {
					destinations[i] = overlayTree.getDestinations().get(i);
				}
				totrack = new HashMap<>();
				for (int i = 0; i < destinations.length; i++) {
					v = overlayTree.findVertexById(destinations[i]);
					totrack.put(v, v.getProxy().getViewManager().getCurrentViewF() + 1);
				}
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				target = overlayTree.lca(destinations).getProxy();
				System.out.println("id ==    " + overlayTree.lca(destinations).getGroupId());
				req = new RequestDirect(type, key, value, destinations, seqNumber, clientId, clientId);
				c.repliesTracker2.put(seqNumber, new RequestTrackerDirect(totrack, null));
				target.invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
				break;
			default:
				System.err.println("Invalid option...");
			}
		}
	}

	/**
	 * Async reply reciever
	 */
	@Override
	public void replyReceived(RequestContext context, TOMMessage reply) {

		RequestDirect replyReq = new RequestDirect(reply.getContent());
//		if (replyReq.getType() == RequestType.NOP)
//			return;
		System.out.println(replyReq.toString());
		//
		RequestTrackerDirect tracker = repliesTracker2.get(replyReq.getSeqNumber());
		// add the reply to tracker and if all involved groups reached their f+1 quota
		if (tracker != null && tracker.addReply(replyReq)) {
			System.out.println("DOOOOONE " + replyReq.getSeqNumber());
			// get reply with all groups replies
			RequestDirect finalReply = tracker.getMergedReply();
			switch (finalReply.getType()) {
			case PUT:
				System.out.println();
				System.out.println();
				for (int i = 0; i < finalReply.getResult().length; i++) {
					System.out.println(

							"previous value at replica " + finalReply.getDestination()[i] + " index " + i+": "
									+ (finalReply.getResult()[i] == null ? "NULL" : new String(finalReply.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;
			case NOP:
				System.out.println("WP");
			case GET:
				System.out.println();
				System.out.println();
				for (int i = 0; i < finalReply.getResult().length; i++) {
					System.out.println(

							"value at replica " + finalReply.getDestination()[i] + " index " + i+": "
									+ (finalReply.getResult()[i] == null ? "NULL" : new String(finalReply.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;
			case REMOVE:
				System.out.println();
				System.out.println();
				for (int i = 0; i < finalReply.getResult().length; i++) {
					System.out.println(

							"removed value at replica " + finalReply.getDestination()[i] + " index " + i+": "
									+ (finalReply.getResult()[i] == null ? "NULL" : new String(finalReply.getResult()[i])));
				}
				System.out.println();
				System.out.println();
				break;
			case SIZE:
				System.out.println();
				System.out.println();
				for (int i = 0; i < finalReply.getResult().length; i++) {
					System.out.println(

							"map size at replica " + finalReply.getDestination()[i] + " index " + i+": "
									+ (finalReply.getResult()[i] == null ? "NULL" : new String(finalReply.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;

			default:
				break;
			}

			// remove finished request tracker
			repliesTracker2.remove(finalReply.getSeqNumber());

		}
	}

	public void handle(TOMMessage msg) {
		try {
			lock.acquire();
				replyReceived(null, msg);
				
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lock.release();
		
	}

	@Override
	public void reset() {

		// TODO reset for reply receiver

	}
}
