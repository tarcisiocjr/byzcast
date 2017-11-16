package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class AMcastReplier implements Replier, FIFOExecutable, Serializable {
    private transient Lock replyLock;
    private transient Condition contextSet;
    private transient ReplicaContext rc;
    private Request req;
    private Map<Integer, byte[]> table;
    private SortedMap<Integer, Vector<TOMMessage>> globalReplies;
    private int group;

    public AMcastReplier(int group) {
        replyLock = new ReentrantLock();
        contextSet = replyLock.newCondition();
        globalReplies = new TreeMap<>();
        table = new TreeMap<>();
        req = new Request();
        this.group = group;
    }

    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        while (rc == null) {
            try {
                this.replyLock.lock();
                this.contextSet.await();
                this.replyLock.unlock();
            } catch (InterruptedException ex) {
                Logger.getLogger(AMcastReplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        req.fromBytes(request.reply.getContent());
        if (req.getDestination().length == 1) {
            rc.getServerCommunicationSystem().send(new int[]{request.getSender()}, request.reply);
        } else {
            int n = rc.getStaticConfiguration().getN();
            byte[] response;

            Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());
            if (msgs.size() < n) {
//                System.out.println("Msg " + req.getSeqNumber() + ": " + msgs.size() + " global replicas so far, waiting for " + n + "...");
                return;
            }

            response = executeSingle(request.getContent(), null, true, true);
            for (TOMMessage msg : msgs) {
                msg.reply.setContent(response);
                rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
            }
        }
    }

    @Override
    public void setReplicaContext(ReplicaContext rc) {
        this.replyLock.lock();
        this.rc = rc;
        this.contextSet.signalAll();
        this.replyLock.unlock();
    }

    @Override
    public byte[] executeOrderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
        return executeSingle(bytes, messageContext, true, false);
    }

    @Override
    public byte[] executeUnorderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
        return executeSingle(bytes, messageContext, false, false);
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        return executeSingle(bytes, messageContext, true, false);
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        return executeSingle(bytes, messageContext, false, false);
    }

    // applies the operation into the TreeMap.
    private byte[] executeSingle(byte[] command, MessageContext msgCtx, boolean ordered, boolean forceExecution) {
        Request req = new Request();
        byte[] resultBytes;

        if (!ordered) {
            System.err.println("Unordered msg: sig = " + msgCtx.getSignature() + ", sender = " + msgCtx.getSender() + ", mac = " + msgCtx.getFirstInBatch().serializedMessageMAC);
            return null;
        }

        req.fromBytes(command);
        if (req.getDestination().length > 1 && !forceExecution) {
            return command;
        }

        switch (req.getType()) {
            case PUT:
                resultBytes = table.put(req.getKey(), req.getValue());
                break;
            case GET:
                resultBytes = table.get(req.getKey());
                break;
            case REMOVE:
                resultBytes = table.remove(req.getKey());
                break;
            case SIZE:
                resultBytes = ByteBuffer.allocate(4).putInt(table.size()).array();
                break;
            default:
                resultBytes = null;
                System.err.println("Unknown request type: " + req.getType());
        }

        req.setValue(resultBytes);
        return req.toBytes();
    }

    private Vector<TOMMessage> saveReply(TOMMessage reply, int seqNumber) {
        Vector<TOMMessage> messages = globalReplies.get(seqNumber);

        if (messages == null) {
            messages = new Vector<>();
            globalReplies.put(seqNumber, messages);
        }

        messages.add(reply);
        return messages;
    }

}
