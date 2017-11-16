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
public class AMcastBatchReplier implements Replier, FIFOExecutable, Serializable {
    private transient Lock replyLock;
    private transient Condition contextSet;
    private transient ReplicaContext rc;
    private Request req;
    private Map<Integer, byte[]> table;
    private SortedMap<Integer, Vector<TOMMessage>> globalReplies;
    private Map<Integer, Request> executedReq;
    private int group;
    private boolean nonGenuine;

    public AMcastBatchReplier(int group, boolean ng) {
        replyLock = new ReentrantLock();
        contextSet = replyLock.newCondition();
        globalReplies = new TreeMap<>();
        executedReq = new TreeMap<>();
        table = new TreeMap<>();
        req = new Request();
        this.group = group;
        nonGenuine = ng;
    }

    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        while (rc == null) {
            try {
                this.replyLock.lock();
                this.contextSet.await();
                this.replyLock.unlock();
            } catch (InterruptedException ex) {
                Logger.getLogger(AMcastBatchReplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        req.fromBytes(request.reply.getContent());
        if (req.getDestination().length == 1 && !nonGenuine) {
            req = execute(req);
            request.reply.setContent(req.toBytes());
            rc.getServerCommunicationSystem().send(new int[]{request.getSender()}, request.reply);
        } else {
            //NON-GENUINE: ALL MESSAGES COME FROM THE GLOBAL GROUP
            int n = rc.getStaticConfiguration().getN();

            Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());
            if (msgs.size() < n) {
                return;
            }

            for (TOMMessage msg : msgs) {
                req.fromBytes(msg.reply.getContent());

                Request[] reqs = Request.ArrayfromBytes(req.getValue());
                //System.out.println("Sender " + msg.getSender() + ": batch #" + req.getSeqNumber() + " of size " + reqs.length);
                for (int i = 0; i < reqs.length; i++) {
                    //System.out.println("Processing request " + reqs[i]);
                    Request temp = executedReq.get(reqs[i].getSeqNumber());
                    if (temp != null) {
                        //System.out.println("Using saved request");
                        reqs[i] = temp;
                    } else {
                        reqs[i] = execute(reqs[i]);
                        executedReq.put(reqs[i].getSeqNumber(), reqs[i]);
                    }
                }

                req.setValue(Request.ArrayToBytes(reqs));
                msg.reply.setContent(req.toBytes());
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
        // executeSingle(bytes, messageContext);
    }

    @Override
    public byte[] executeUnorderedFIFO(byte[] bytes, MessageContext messageContext, int i, int i1) {
        if (!nonGenuine)
            new UnsupportedOperationException("Genuine batch replier only accepts ordered messages");
        return bytes;
        //return executeSingle(bytes, messageContext);
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("All ordered messages should be FIFO");
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("All unordered messages should be FIFO");
    }

    protected Request execute(Request req) {
        byte[] resultBytes;
        boolean toMe = false;

        for (int i = 0; i < req.getDestination().length; i++) {
            if (req.getDestination()[i] == group) {
                toMe = true;
                break;
            }
        }

        if (!toMe) {
            //System.out.println("Message not addressed to my group.");
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

    protected Vector<TOMMessage> saveReply(TOMMessage reply, int seqNumber) {
        Vector<TOMMessage> messages = globalReplies.computeIfAbsent(seqNumber, k -> new Vector<>());
        messages.add(reply);
        return messages;
    }
}
