package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.core.messages.TOMMessage;
import ch.usi.inf.dslab.bftamcast.kvs.Request;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class AmcastLocalBatchReplier extends AmcastLocalReplier {
    private Map<Integer, Request> executedReq;
    private boolean nonGenuine;

    public AmcastLocalBatchReplier(int group, boolean ng) {
        super(group);
        executedReq = new TreeMap<>();
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
                Logger.getLogger(AmcastLocalBatchReplier.class.getName()).log(Level.SEVERE, null, ex);
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
}
