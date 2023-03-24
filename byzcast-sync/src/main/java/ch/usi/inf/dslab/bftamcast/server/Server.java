package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.io.*;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Server extends DefaultRecoverable /*implements FIFOExecutable*/ {
    private LocalBatchReplier replier;
    private int id, groupId, nextTS;

    public Server(int id, int group, String configPath, boolean nonGenuine) {
        this.id = id;
        this.groupId = group;
        this.nextTS = 0;
        replier = new LocalBatchReplier(groupId, nonGenuine);

        try {
            Thread.sleep(this.id * 1500);
        } catch (InterruptedException e) {
            System.err.println("Error starting server " + this.id);
            e.printStackTrace();
            System.exit(-1);
        }

        new ServiceReplica(this.id, configPath, replier, this, null, replier);
    }

    public static void main(String[] args) {
        CLIParser p = CLIParser.getLocalServerParser(args);
        new Server(p.getId(), p.getGroup(), p.getLocalConfig(), p.isNonGenuine());
    }


    // TreeMap to byte array
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


    // Byte array to TreeMap
    @Override
    public void installSnapshot(byte[] state) {
        ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInput in = new ObjectInputStream(bis);
            replier = (LocalBatchReplier) in.readObject();
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
    public byte[][] appExecuteBatch(byte[][] bytes, MessageContext[] messageContexts) {
        throw new UnsupportedOperationException("Implemented by LocalReplier");
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("Implemented by LocalReplier");
    }
}