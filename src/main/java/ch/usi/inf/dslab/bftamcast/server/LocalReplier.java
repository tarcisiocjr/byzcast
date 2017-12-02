package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;

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
public class LocalReplier implements Replier, FIFOExecutable, Serializable {
    transient Lock replyLock;
    transient Condition contextSet;
    transient ReplicaContext rc;
    Request req;
    private Map<Integer, byte[]> table;
    private int group;
    private SortedMap<Integer, Vector<TOMMessage>> globalReplies;

    LocalReplier(int group) {
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
                Logger.getLogger(LocalReplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        req.fromBytes(request.reply.getContent());
        if (req.getDestination().length == 1) {
            req = execute(req);
            request.reply.setContent(req.toBytes());
            rc.getServerCommunicationSystem().send(new int[]{request.getSender()}, request.reply);
        } else {
            int n = rc.getStaticConfiguration().getN();
            byte[] response;

            Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());
            if (msgs.size() < n) {
                return;
            }

            req = execute(req);
            response = req.toBytes();
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
        return bytes;
    }

    @Override
    public byte[] executeUnorderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
        throw new UnsupportedOperationException("AMcast replier only accepts ordered messages");
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("All ordered messages should be FIFO");
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("All ordered messages should be FIFO");
    }

    Request execute(Request req) {
        byte[] resultBytes;
        boolean toMe = false;

        for (int i = 0; i < req.getDestination().length; i++) {
            if (req.getDestination()[i] == group) {
                toMe = true;
                break;
            }
        }

        if (!toMe && req.getType() != RequestType.BATCH) {
            //System.out.println("Message not addressed to my group: " + req);
            req.setType(RequestType.NOP);
            req.setValue(null);
        } else {
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
        }
        return req;
    }

    Vector<TOMMessage> saveReply(TOMMessage reply, int seqNumber) {
        Vector<TOMMessage> messages = globalReplies.computeIfAbsent(seqNumber, k -> new Vector<>());
        messages.add(reply);
        return messages;
    }

    void deleteReply(int seqNumber) {
        globalReplies.remove(seqNumber);
    }

    Vector<TOMMessage> getReply(int seqNumber) {
        return globalReplies.get(seqNumber);
    }
}
