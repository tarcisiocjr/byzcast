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
    private SortedMap<Integer, Request> executedReq;


    private SortedMap<Integer, TOMMessage> pending;
    private SortedMap<Integer, Integer> innerReplies;


    GlobalBatchReplier(BatchServerGlobal server) {
        super(server.getGroupId());
        this.server = server;
        remaining = 0;
        requests = new Vector<>();
        batchRequests = new TreeMap<>();
        executedReq = new TreeMap<>();
        pending = new TreeMap<>();
        innerReplies = new TreeMap<>();
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
        int N = rc.getStaticConfiguration().getN();

        remaining--;
        req.fromBytes(request.reply.getContent());
        if (req.getType() == RequestType.BATCH) {
            //System.out.println("Message from TOP LEVEL: " + req);
            pending.remove(request.getSender());
            pending.put(request.getSender(), request);

            Request[] reqs = Request.ArrayfromBytes(req.getValue());
            int total;
            boolean checkAll = false;
            for (Request r :
                    reqs) {
                total = innerReplies.compute(r.getSeqNumber(), (k, v) -> (v == null) ? 1 : v + 1);
                //System.out.println("Partial total = " + total + ", req = " + r + " , sender = " +
                //        request.getSender() + ", #msgs = " + reqs.length);
                if (total == N)
                    checkAll = true;
            }

            if (checkAll) {
                for (TOMMessage msg : pending.values()) {
                    Request batch = new Request();
                    batch.fromBytes(msg.reply.getContent());
                    reqs = Request.ArrayfromBytes(batch.getValue());
                    total = 0;
                    //System.out.println("Checking batch " + batch + " to sender " + msg.getSender() + " with size " + reqs.length);
                    for (Request r : reqs) {
                        Integer i = innerReplies.get(r.getSeqNumber());
                        if (i != null)
                            total += i;
                        //System.out.println("total = " + total + " for req " + r.getSeqNumber());
                    }
                    //System.out.println("Total = " + total + " / " + N);
                    if (total == reqs.length * N) {
                        //System.out.println("can reply this: " + batch + " to sender " + msg.getSender());
                        for (int i = 0; i < reqs.length; i++) {
                            batchRequests.put(reqs[i].getSeqNumber(), reqs[i].clone());
                        }
                        //} else {
                        //System.out.println("CANNOT forward yet\n\n");
                    }
                }
                //} else {
                //System.out.println("Total = " + total + " / " + N);
            }


            batchRequests.entrySet().removeIf(entry -> executedReq.containsKey(entry.getKey()));
            for (Request r : batchRequests.values())
                if (!requests.contains(r))
                    requests.add(r);
            //requests.addAll(batchRequests.values());

            //for (int i : batchRequests.keySet())
            //System.out.println("key = " + i);

        } else {
            req.setSeqNumber(server.getNextInnerSeqNumber()); // set request's seq number
            //System.out.println("Message from a CLIENT: " + req);
            saveReply(request, req.getSeqNumber());
            requests.add(req.clone());
        }

        //sending....
        if (remaining == 0 && requests.size() > 0) {
            //System.out.println("Request size = " + requests.size());
            //for (Request r : requests)
            //System.out.println("\tTO SEND: " + r);

            Request[] responses = server.send(requests.toArray(new Request[0]));
            requests.clear();

            for (Request r :
                    responses) {
                //System.out.println("Received response: " + r);
                Vector<TOMMessage> msgVector = getReply(r.getSeqNumber());


                if (innerReplies.containsKey(r.getSeqNumber())) {
                    //System.out.println("message for top-level batch");
                    executedReq.put(r.getSeqNumber(), r);
                } else {
                    //System.out.println("Replying to client");
                    TOMMessage msg = msgVector.get(0);
                    msg.reply.setContent(r.toBytes());
                    rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
                }

            }
            for (TOMMessage msg : pending.values()) {

                //if (batchId > 0) {
                //Vector<TOMMessage> msgVector = getReply(batchId);
                //for (TOMMessage msg : msgVector) {
                Request batch = new Request();
                batch.fromBytes(msg.reply.getContent());
                //System.out.println("checking batch = " + batch);
                Request[] inReqs = Request.ArrayfromBytes(batch.getValue());
                int i;
                for (i = 0; i < inReqs.length; i++) {
                    try {
                        //System.out.println(msg.getSender() + " added to batch: " + inReqs[i]);
                        inReqs[i].setValue(executedReq.get(inReqs[i].getSeqNumber()).getValue());
                    } catch (NullPointerException e) {
                        //System.out.println("ERROR: batch with non-replied message. trying next");
                        break;
                        //System.exit(1);
                    }
                }

                if (i == inReqs.length) {
                    batch.setValue(Request.ArrayToBytes(inReqs));
                    msg.reply.setContent(batch.toBytes());
                    //System.out.println("Replying to top-level server " + msg.getSender());
                    rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
                }
            }
            batchRequests.clear();
        }
    }

    void setBatchSize(int size) {
        remaining += size;
        //System.out.println("New batch of size " + size + ", remaining " + remaining);
    }
}
