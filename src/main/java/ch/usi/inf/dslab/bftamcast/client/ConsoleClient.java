package ch.usi.inf.dslab.bftamcast.client;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;
import ch.usi.inf.dslab.bftamcast.util.GroupRequestTracker;

public class ConsoleClient implements ReplyListener {
	final Map<Integer, GroupRequestTracker> repliesTracker = new HashMap<>();
	private static Scanner scanner;

	public static void main(String[] args) {
		int seqNumber = 0;
		ConsoleClient c = new ConsoleClient();
		CLIParser p = CLIParser.getClientParser(args);
		Random r = new Random();
		int clientId = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
		String treeConfigPath = p.getTreeConfig();
		Tree overlayTree = new Tree(treeConfigPath, UUID.randomUUID().hashCode());
		Request req;

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
			Vertex target;
			seqNumber++;
			byte[] value;
			int[] destinations;
			int key;
			RequestType type;
			long destIdentifier = 0;

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
				value = console.readLine("Enter the value: ").getBytes();
				destIdentifier = overlayTree.getIdentifier(destinations);
				target = overlayTree.getLca(destIdentifier);

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId, destIdentifier);

				System.out.println("id ==    " + target.getID());
				c.repliesTracker.put(seqNumber,
						new GroupRequestTracker(target.getProxy().getViewManager().getCurrentViewF() + 1));
				target.getProxy().invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
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
				destIdentifier = overlayTree.getIdentifier(destinations);
				target = overlayTree.getLca(destIdentifier);

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId, destIdentifier);

				System.out.println("id ==    " + target.getID());
				c.repliesTracker.put(seqNumber,
						new GroupRequestTracker(target.getProxy().getViewManager().getCurrentViewF() + 1));
				target.getProxy().invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
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
				destIdentifier = overlayTree.getIdentifier(destinations);
				target = overlayTree.getLca(destIdentifier);

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId, destIdentifier);

				System.out.println("id ==    " + target.getID());
				c.repliesTracker.put(seqNumber,
						new GroupRequestTracker(target.getProxy().getViewManager().getCurrentViewF() + 1));
				target.getProxy().invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
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
				destIdentifier = overlayTree.getIdentifier(destinations);
				target = overlayTree.getLca(destIdentifier);

				req = new Request(type, key, value, destinations, seqNumber, clientId, clientId, destIdentifier);

				System.out.println("id ==    " + target.getID());
				c.repliesTracker.put(seqNumber,
						new GroupRequestTracker(target.getProxy().getViewManager().getCurrentViewF() + 1));
				target.getProxy().invokeAsynchRequest(req.toBytes(), c, TOMMessageType.ORDERED_REQUEST);
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
		System.out.println("recieved");

		Request replyReq = new Request(reply.getContent());
		GroupRequestTracker tracker = repliesTracker.get(replyReq.getSeqNumber());
		if (tracker != null && tracker.addReply(reply)) {
			replyReq = tracker.getMajorityReply();
			System.out.println("finish, sent up req # " + replyReq.getSeqNumber());

			repliesTracker.remove(replyReq.getSeqNumber());
			switch (replyReq.getType()) {
			case PUT:
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"previous value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}

				break;
			case GET:
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}

				break;
			case REMOVE:
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"removed value at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}
				break;
			case SIZE:
				for (int i = 0; i < replyReq.getResult().length; i++) {
					System.out.println(

							"map size at replica " + replyReq.getDestination()[i] + " : "
									+ (replyReq.getResult()[i] == null ? "NULL" : new String(replyReq.getResult()[i])));
				}

				break;

			default:
				break;
			}
		}

	}

	@Override
	public void reset() {
		// TODO reset for reply receiver

	}
}
