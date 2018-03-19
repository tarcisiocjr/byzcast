/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import ch.usi.inf.dslab.bftamcast.graph.Tree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Server extends DefaultRecoverable {
	private ReplicaReplier replier;
	private int id, groupId, nextTS;
	private int seqN = 0;

	/**
	 * main for launching group member
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CLIParser p = CLIParser.getServerParser(args);
		new Server(p.getId(), p.getGroup(), p.getGroupConfig(), p.getTreeConfig());
	}

	/**
	 * constructor
	 * 
	 * @param id
	 *            of the replica
	 * @param group
	 *            id the replica belongs to
	 * @param configPath
	 *            path for bftsmart for the replica (//TODO could extract that from
	 *            tree config)
	 * @param treeConfigPath
	 *            path of the configuration file representing the overlay tree of
	 *            groups
	 */
	public Server(int id, int group, String configPath, String treeConfigPath) {
		this.id = id;
		this.groupId = group;
		this.nextTS = 0;
		replier = new ReplicaReplier(id, group, treeConfigPath);

		try {
			Thread.sleep(this.groupId * 4000 + this.id * 1000);
		} catch (InterruptedException e) {
			System.err.println("Error starting server " + this.id);
			e.printStackTrace();
			System.exit(-1);
		}

		new ServiceReplica(this.id, configPath, replier, this, null, replier);
	}

	/**
	 * install snapshot for this replica //TODO add needed fields when batching is
	 * done
	 */
	@Override
	public void installSnapshot(byte[] state) {
		ByteArrayInputStream bis = new ByteArrayInputStream(state);
		try {
			ObjectInput in = new ObjectInputStream(bis);
			replier = (ReplicaReplier) in.readObject();
			id = in.readInt();
			groupId = in.readInt();
			nextTS = in.readInt();
			in.close();
			bis.close();
		} catch (ClassNotFoundException e) {
			System.out.print("Couldn't find Map: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.print("Exception installing the application state: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * get snapshot for this replica //TODO add needed fields when batching is done
	 */
	@Override
	public byte[] getSnapshot() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(replier);
			out.writeInt(id);
			out.writeInt(groupId);
			out.writeInt(nextTS);
			out.flush();
			out.close();
			bos.close();
			return bos.toByteArray();
		} catch (IOException e) {
			System.out.println(
					"Exception when trying to take a + " + "snapshot of the application state" + e.getMessage());
			e.printStackTrace();
			return new byte[0];
		}
	}

//	/**
//	 * Execute batch of requests, not sure it make sense, but I still have to read
//	 * how it works and check what paulo did
//	 */
//	@Override
//	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
//
//		// do it in the replier and client??
//		// how to build batch for mixed clients?
//
//		// TODO extract requests, make the replier handle them
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		byte[][] replies = new byte[commands.length][];
//		Request[] reqs = new Request[commands.length];
//		for (int i = 0; i < reqs.length; i++) {
//			reqs[i] = new Request(commands[i]);
//		}
//
//		// TODO prepare batch for children
//		Map<Vertex, List<Request>> replicaRequests = new HashMap<>();
//		Tree overlayTree = replier.getOverlayTree();
//		for (Request req : reqs) {
//			// forward to me
//			for (int dest : req.getDestination()) {
//				if (groupId == dest) {
//					replicaRequests.computeIfAbsent(replier.getVertex(), k -> new ArrayList<>());
//					replicaRequests.get(replier.getVertex()).add(req);
//				} else if (replier.getVertex().getChildernIDs().contains(dest)) {
//					Vertex v = overlayTree.findVertexById(dest);
//					replicaRequests.computeIfAbsent(v, k -> new ArrayList<>());
//					replicaRequests.get(v).add(req);
//				}
//
//				else {
//					for (Vertex v : replier.getVertex().getChildren()) {
//						if (v.inReach(dest)) {
//							if (!replicaRequests.containsKey(v) || !replicaRequests.get(v).contains(req)) {
//								replicaRequests.computeIfAbsent(v, k -> new ArrayList<>());
//								replicaRequests.get(v).add(req);
//							}
//							break;// only one path
//						}
//					}
//				}
//			}
//		}
//
//		// batch identical destinations requests?
//
//		// if destinations are different need a different tracker for eachone?? do that
//		// in replier?
//
//		Thread[] threads = new Thread[replicaRequests.keySet().size()];
//		Request[] threadsReplies = new Request[overlayTree.getDestinations().size()];
//
//		int i = 0;
//		for (Vertex dest : replicaRequests.keySet()) {
//			Request batchReq = new Request(RequestType.BATCH, 0,
//					Request.ArrayToBytes(replicaRequests.get(dest).toArray(new Request[0])),
//					new int[] { dest.getGroupId() }, seqN++, groupId, groupId);
//			// can use async but can not access to reply inside this method, while have to
//			// wait until callback (same as doing synchronous)
//			//how merge replies from multiple replicas??? you get only one!? proxy with asyn produce f+1 answers, but invokeordered only one, where to check??
//			threads[i] = new Thread(
//					() -> threadsReplies[dest.getGroupId()] = new Request(dest.getProxy().invokeOrdered(batchReq.toBytes())));
//
//			threads[i].start();
//			i++;
//		}
//
//		for (int j = 0; j < threads.length; j++) {
//			try {
//				threads[j].join();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//		// merge replies for same requests 
//		
//		//continue after talking with paulo
//
//		
//		throw new UnsupportedOperationException("Implemented by UniversalReplier");
//	}
	
	
	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {

		// do it in the replier and client??
		// how to build batch for mixed clients?

		// TODO extract requests, make the replier handle them
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Request[] batch = new Request[commands.length];
		for (int i = 0; i < batch.length; i++) {
			batch[i] = new Request(commands[i]);
		}
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		System.out.println("BAAAAATCH");
		return replier.handleBatch(batch);
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

}
