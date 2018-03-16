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
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Server extends DefaultRecoverable{
	private ReplicaReplier replier;
	private int id, groupId, nextTS;
	
	/**
	 * main for launching group member
	 * @param args
	 */
	public static void main(String[] args) {
        CLIParser p = CLIParser.getServerParser(args);
        new Server(p.getId(), p.getGroup(), p.getGroupConfig(), p.getTreeConfig());
    }
	
		
	/**
	 * constructor
	 * @param id  of the replica
	 * @param group id the replica belongs to
	 * @param configPath path for bftsmart for the replica (//TODO could extract that from tree config)
	 * @param treeConfigPath path of the configuration file representing the overlay tree of groups
	 */
	public Server(int id, int group, String configPath, String treeConfigPath) {
		this.id = id;
        this.groupId = group;
        this.nextTS = 0;
        replier = new ReplicaReplier(id, group,treeConfigPath);

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
	 * install snapshot for this replica //TODO add needed fields when batching is done
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
            System.out.println("Exception when trying to take a + "
                    + "snapshot of the application state" + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
	}

	/**
	 * Execute batch of requests, not sure it make sense, but I still have to read how it works and check what paulo did
	 */
	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
		//TODO extract requests, make the replier handle them
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[][] replies = new byte[commands.length][];
		Request[] reqs = new Request[commands.length];
		for (int i = 0; i < reqs.length; i++) {
			reqs[i] = new Request(commands[i]);
		}
		
		Map<Integer, List<Request>> replicaRequests = new HashMap<>();
		for(Request r : reqs) {
			
		}
		
//		Paulo code:
//				ByteArrayOutputStream bos = new ByteArrayOutputStream();
//				byte[][] replies = new byte[command.length][];
//				Request mainReq = new Request(), auxReq = new Request();
//				Request[] reqs = new Request[command.length];
//
//				try {
//					mainReq.setType(RequestType.BATCH);
//					mainReq.setDestination(allDest);
//					mainReq.setSeqNumber(seqNumber++);
//					// System.out.println("batch size = " + command.length + ", seq. number = " +
//					// mainReq.getSeqNumber());
//
//					for (int i = 0; i < reqs.length; i++) {
//						reqs[i] = new Request();
//						reqs[i].fromBytes(command[i]);
//						reqs[i].setSeqNumber(innerSeqNumber++);
//					}
//
//					mainReq.setValue(Request.ArrayToBytes(reqs));
//					for (int dest : allDest) {
//						invokeThreads[dest] = new Thread(
//								() -> invokeReplies[dest] = proxiesToLocal[dest].invokeOrdered(mainReq.toBytes()));
//						invokeThreads[dest].start();
//					}
//
//					// reset values
//					for (int i = 0; i < command.length; i++)
//						reqs[i].setValue(null);
//
//					for (int dest : allDest) {
//						invokeThreads[dest].join();
//						auxReq.fromBytes(invokeReplies[dest]);
//						Request[] temp = Request.ArrayfromBytes(auxReq.getValue());
//						// System.out.println("reply from group " + dest + ": req = " + auxReq + ",
//						// reply size = " + temp.length);
//						for (int i = 0; i < temp.length; i++) {
//							bos.reset();
//							if (temp[i].getType() != RequestType.NOP) { // message was addressed to group dest
//								if (reqs[i].getValue() == null) { // set value initially to false
//									reqs[i].setValue(temp[i].getValue());
//								} else {
//									bos.write(reqs[i].getValue());
//									bos.write(temp[i].getValue());
//									bos.close();
//									reqs[i].setValue(bos.toByteArray());
//								}
//							}
//						}
//						auxReq.setValue(null);
//					}
//
//					for (int i = 0; i < command.length; i++)
//						replies[i] = reqs[i].toBytes();
//
//				} catch (InterruptedException | IOException | ArrayIndexOutOfBoundsException e) {
//					e.printStackTrace();
//					System.exit(20);
//				}
//
//				return replies;
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

	
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

}
