package ch.usi.inf.dslab.bftamcast.client;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.TreeDirect;
import ch.usi.inf.dslab.bftamcast.graph.VertexDirect;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;
import ch.usi.inf.dslab.bftamcast.util.GroupRequestTracker;
import ch.usi.inf.dslab.bftamcast.util.RequestTracker;
import ch.usi.inf.dslab.bftamcast.util.RequestTrackerDirect;

public class ConsoleClientDirect implements ReplyListener {
	final Map<Integer, GroupRequestTracker> repliesTracker = new HashMap<>();
	private static Scanner scanner;
	final Map<Integer, RequestTrackerDirect> repliesTracker2 = new HashMap<>();

	public static void main(String[] args) {
		int seqNumber = 0;
		ConsoleClientDirect c = new ConsoleClientDirect();
		CLIParser p = CLIParser.getClientParser(args);
		int clientId = UUID.randomUUID().hashCode();
		String treeConfigPath = p.getTreeConfig();
		TreeDirect overlayTree = new TreeDirect(treeConfigPath, clientId, c);
		Request req;

		for (Integer i : overlayTree.getDestinations()) {
			VertexDirect v = overlayTree.findVertexById(i);
			v.getProxy().invokeAsynchRequest(
					new Request(RequestType.NOP, -1, new byte[0], new int[0], 0, 0, 0).toBytes(), c,
					TOMMessageType.ORDERED_REQUEST);
		}
		//

		Console console = System.console();
		scanner = new Scanner(System.in);
		while (true) {
			// for (Integer i : overlayTree.getDestinations()) {
			// Vertex v = overlayTree.findVertexById(i);
			// v.getProxy().invokeAsynchRequest(new Request(RequestType.NOP, -1, new
			// byte[0], new int[0], 0, 0, 0).toBytes(), c, TOMMessageType.ORDERED_REQUEST);
			// }
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
			Map<VertexDirect, Integer> totrack;

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

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId);

				target = overlayTree.lca(n).getProxy();
				c.repliesTracker.put(seqNumber, new GroupRequestTracker(target.getViewManager().getCurrentViewF() + 1));
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
				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId);
				// c.repliesTracker.put(seqNumber, new
				// GroupRequestTracker(target.getViewManager().getCurrentViewF() + 1));
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
				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId);
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
				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId);
				c.repliesTracker.put(seqNumber, new GroupRequestTracker(target.getViewManager().getCurrentViewF() + 1));
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

		Request replyReq = new Request(reply.getContent());
		if (replyReq.getType() == RequestType.NOP)
			return;
		System.out.println(replyReq.toString());
		//
		RequestTrackerDirect tracker = repliesTracker2.get(replyReq.getSeqNumber());
		// add the reply to tracker and if all involved groups reached their f+1 quota
		if (tracker != null && tracker.addReply(replyReq)) {
			System.out.println("DOOOOONE " + replyReq.getSeqNumber());
			// get reply with all groups replies
			Request finalReply = tracker.getMergedReply();
			switch (finalReply.getType()) {
			case PUT:
				System.out.println();
				System.out.println();
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"previous value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;
			case GET:
				System.out.println();
				System.out.println();
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;
			case REMOVE:
				System.out.println();
				System.out.println();
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"removed value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}
				System.out.println();
				System.out.println();
				break;
			case SIZE:
				System.out.println();
				System.out.println();
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"map size at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}
				System.out.println();
				System.out.println();

				break;

			default:
				break;
			}

			// remove finished request tracker
			repliesTracker2.remove(replyReq.getSeqNumber());

		}
	}

	public void handle(TOMMessage msg) {
		 System.out.println("GG");
		Request replyReq = new Request(msg.getContent());
		replyReceived(null, msg);
	}

	@Override
	public void reset() {

		// TODO reset for reply receiver

	}
}
