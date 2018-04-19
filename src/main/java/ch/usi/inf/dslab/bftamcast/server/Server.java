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

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Server extends DefaultRecoverable {
	private ReplicaReplier replier;
	private int id, groupId;

	/**
	 * main for launching group member
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CLIParser p = CLIParser.getServerParser(args);
		new Server(p.getId(), p.getGroup(), p.getTreeConfig(), p.getOutstandingMsg());
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
	public Server(int id, int group, String treeConfigPath, int maxOutstanding) {
		this.id = id;
		this.groupId = group;
		replier = new ReplicaReplier(id, group, treeConfigPath, maxOutstanding);

		try {
			Thread.sleep(this.groupId * 4000 + this.id * 1000);
		} catch (InterruptedException e) {
			System.err.println("Error starting server " + this.id);
			e.printStackTrace();
			System.exit(-1);
		}

//		System.out.println(replier.getMyVertex().getConfPath());
		new ServiceReplica(this.id, replier.getMyVertex().getConfPath(), replier, this, null, replier);
	}

	/**
	 * install snapshot for this replica
	 */
	@Override
	public void installSnapshot(byte[] state) {
		ByteArrayInputStream bis = new ByteArrayInputStream(state);
		try {
			ObjectInput in = new ObjectInputStream(bis);
			replier = (ReplicaReplier) in.readObject();
			id = in.readInt();
			groupId = in.readInt();
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
	 * get snapshot for this replica
	 */
	@Override
	public byte[] getSnapshot() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(replier);
			out.writeInt(id);
			out.writeInt(groupId);
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

	
	
	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

}
