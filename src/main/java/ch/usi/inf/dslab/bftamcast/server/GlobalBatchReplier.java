package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class GlobalBatchReplier extends LocalReplier {
    private int remaining;
    private Vector<Request> requests;
    private BatchServerGlobal server;
    private SortedMap<Integer, Request> batchRequests;
    private int batchId;
    private SortedMap<Integer, Request> executedReq;


    GlobalBatchReplier(BatchServerGlobal server) {
        super(server.getGroupId());
        this.server = server;
        remaining = 0;
        requests = new Vector<>();
        batchRequests = new TreeMap<>();
        executedReq = new TreeMap<>();
        batchId = -1;
    }

    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        while (rc == null) {
            try {
                this.replyLock.lock();
                this.contextSet.await();
                this.replyLock.unlock();
            } catch (InterruptedException ex) {
                Logger.getLogger(GlobalBatchReplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        remaining--;
        req.fromBytes(request.reply.getContent());
        if (req.getType() == RequestType.BATCH) {
           // System.out.println("Message from TOP LEVEL: " + req);
            Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());
            saveRequests(req);
            if (msgs.size() < rc.getStaticConfiguration().getN()) {
                //System.out.println("Not enough replicas: " + msgs.size());
                return;
            } else {
                //System.out.println("All replies. Batch requests are: ");
                //for (Request r : batchRequests.values())
                 //   System.out.println("request = " + r);

                batchId = req.getSeqNumber();
                batchRequests.entrySet().removeIf(entry -> executedReq.containsKey(entry.getKey()));

                requests.addAll(batchRequests.values());
            }
        } else {
            req.setSeqNumber(server.getNextInnerSeqNumber()); // set request's seq number
           // System.out.println("Message from a CLIENT: " + req);
            saveReply(request, req.getSeqNumber());
            requests.add(req.clone());
        }

        //sending....
        if (remaining == 0 && requests.size() > 0) {
            //System.out.println("Request size = " + requests.size());
            //for (Request r :
             //       requests) {
            //    System.out.println("\tTO SEND: " + r);
           // }

            Request[] responses = server.send(requests.toArray(new Request[0]));
            requests.clear();

            for (Request r :
                    responses) {
                //System.out.println("Received response: " + r);
                Vector<TOMMessage> msgVector = getReply(r.getSeqNumber());

                //for (int i :
                //        batchRequests.keySet()) {
                    //System.out.println("key = " + i);
                //}

                if (batchRequests.containsKey(r.getSeqNumber())) {
                    //System.out.println("message for top-level batch");
                    executedReq.put(r.getSeqNumber(), r);
                } else {
                    //System.out.println("Replying to client");
                    TOMMessage msg = msgVector.get(0);
                    msg.reply.setContent(r.toBytes());
                    rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
                }

            }
            if (batchId > 0) {
                //System.out.println("sending pending batch " + batchId);
                Vector<TOMMessage> msgVector = getReply(batchId);
                for (TOMMessage msg : msgVector) {
                    Request batch = new Request();
                    batch.fromBytes(msg.reply.getContent());
                    //System.out.println("batch = " + batch);
                    Request[] inReqs = Request.ArrayfromBytes(batch.getValue());

                    for (int i = 0; i < inReqs.length; i++) {
                        try {
                            //System.out.println(msg.getSender() + " added to batch: " + inReqs[i]);
                            inReqs[i].setValue(executedReq.get(inReqs[i].getSeqNumber()).getValue());
                        } catch (NullPointerException e) {
                            //System.out.println("FATAL ERROR: batch with non-replied message.");
                            System.exit(1);
                        }
                    }

                    batch.setValue(Request.ArrayToBytes(inReqs));
                    msg.reply.setContent(batch.toBytes());
                    //System.out.println("Replying to top-level server " + msg.getSender());
                    rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
                }
            }
            batchId = -1;
            batchRequests.clear();
        }
    }

    void setBatchSize(int size) {
        remaining += size;
        //System.out.println("New batch of size " + size + ", remaining " + remaining);
    }

    private void saveRequests(Request batch) {
        Request[] reqs = Request.ArrayfromBytes(batch.getValue());
        for (Request r :
                reqs) {
            batchRequests.put(r.getSeqNumber(), r);
        }
    }
}
