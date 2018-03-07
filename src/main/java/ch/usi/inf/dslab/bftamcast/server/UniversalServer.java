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
public class UniversalServer extends DefaultRecoverable{
	private UniversalReplier replier;
	private int id, groupId, nextTS;
	
	public static void main(String[] args) {
        CLIParser p = CLIParser.getServerParser(args);
        new UniversalServer(p.getId(), p.getGroup(), p.getGroupConfig(), p.getTreeConfig());
    }
	
	
	
	
	public UniversalServer(int id, int group, String configPath, String treeConfigPath) {
		this.id = id;
        this.groupId = group;
        this.nextTS = 0;
        replier = new UniversalReplier(id, group,treeConfigPath);

        try {
            Thread.sleep(this.groupId * 4000 + this.id * 1000);
        } catch (InterruptedException e) {
            System.err.println("Error starting server " + this.id);
            e.printStackTrace();
            System.exit(-1);
        }

        new ServiceReplica(this.id, configPath, replier, this, null, replier);
	}

	@Override
	public void installSnapshot(byte[] state) {
		ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInput in = new ObjectInputStream(bis);
            replier = (UniversalReplier) in.readObject();
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

	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		throw new UnsupportedOperationException("Implemented by UniversalReplier");
	}

}
