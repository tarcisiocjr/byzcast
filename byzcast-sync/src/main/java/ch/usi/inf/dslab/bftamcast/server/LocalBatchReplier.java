package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class LocalBatchReplier extends LocalReplier {
    private Map<Integer, Request> executedReq;
    private boolean nonGenuine;
    private SortedMap<Integer, Integer> innerReplies;
    private SortedMap<Integer, TOMMessage> pending;


    public LocalBatchReplier(int group, boolean ng) {
        super(group);
        executedReq = new TreeMap<>();
        nonGenuine = ng;
        innerReplies = new TreeMap<>();
        pending = new TreeMap<>();
    }

    @Override
    public void manageReply(TOMMessage request, MessageContext msgCtx) {
        while (rc == null) {
            try {
                this.replyLock.lock();
                this.contextSet.await();
                this.replyLock.unlock();
            } catch (InterruptedException ex) {
                Logger.getLogger(LocalBatchReplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        int N = rc.getStaticConfiguration().getN();

        req.fromBytes(request.reply.getContent());
        if (req.getDestination().length == 1 && !nonGenuine) {
            req = execute(req);
            request.reply.setContent(req.toBytes());
            rc.getServerCommunicationSystem().send(new int[]{request.getSender()}, request.reply);
        } else {
            //NON-GENUINE: ALL MESSAGES COME FROM THE GLOBAL GROUP
            pending.remove(request.getSender());
            pending.put(request.getSender(), request);
            //Vector<TOMMessage> msgs = saveReply(request, req.getSeqNumber());

            req.fromBytes(request.reply.getContent());
            Request[] reqs = Request.ArrayfromBytes(req.getValue());
            int total = 0;
            boolean checkAll = false;
            for (Request r :
                    reqs) {
                total = innerReplies.compute(r.getSeqNumber(), (k, v) -> (v == null) ? 1 : v + 1);
//                System.out.println("partial total = " + total + ", req = " + r + " , sender = " +
//                       request.getSender() + ", #msgs = " + reqs.length);
                if (total == N)
                    checkAll = true;
            }

            if (checkAll) {
                for (TOMMessage msg : pending.values()) {
                    Request batch = new Request();
                    batch.fromBytes(msg.reply.getContent());
                    reqs = Request.ArrayfromBytes(batch.getValue());
                    total = 0;
//                    System.out.println("Checking batch " + batch + " to sender " + msg.getSender() + " with size " + reqs.length);
                    for (Request r : reqs) {
                        Integer i = innerReplies.get(r.getSeqNumber());
                        if (i != null)
                            total += i;
//                        System.out.println("total = " + total + " for req " + r.getSeqNumber());
                    }
//                    System.out.println("Total = " + total + " / " + N);
                    if (total == reqs.length * N) {
                        //System.out.println("can reply this: " + batch + " to sender " + msg.getSender());
                        for (int i = 0; i < reqs.length; i++) {
//                            System.out.println("Processing request " + reqs[i]);
                            Request temp = executedReq.get(reqs[i].getSeqNumber());
                            if (temp != null) {
//                                System.out.println("Using saved request");
                                reqs[i] = temp;
                            } else {
//                                System.out.println("Executing");
                                reqs[i] = execute(reqs[i]);
                                executedReq.put(reqs[i].getSeqNumber(), reqs[i]);
                            }
                        }

                        batch.setValue(Request.ArrayToBytes(reqs));
                        msg.reply.setContent(batch.toBytes());
                        rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
                        //} else {
                        //System.out.println("CANNOT reply " + batch);
                    }
                }
                //} else {
                //System.out.println("Total = " + total + " / " + N);
            }
        }
    }
}
