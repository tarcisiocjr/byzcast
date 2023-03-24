package ch.usi.inf.dslab.byzcast.client;

import ch.usi.inf.dslab.byzcast.kvs.Request;
import ch.usi.inf.dslab.byzcast.kvs.RequestType;
import ch.usi.inf.dslab.byzcast.util.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */
public class ClientThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientThread.class);

    final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    final int clientId;
    final int groupId;
    final boolean verbose;
    final int runTime;
    final int size;
    final int globalPerc;
    final Random random;
    final Stats localStats, globalStats;
    final int clientCount;
    private final String[] msgDestination;

    final boolean singleRequest;
    ProxyIf proxy;


    public ClientThread(int clientId, int groupId, String lcaConfig, boolean verbose, int runTime, int valueSize, int globalPerc, int clientCount, String[] msgDestination, boolean singleRequest) {
        this.clientId = clientId;
        this.clientCount = clientCount;
        this.groupId = groupId;
        this.verbose = verbose;
        this.runTime = runTime;
        this.size = valueSize;
        this.globalPerc = globalPerc;
        this.random = new Random();
        this.localStats = new Stats();
        this.globalStats = new Stats();
        this.msgDestination = msgDestination;
        this.singleRequest = singleRequest;
        this.proxy = new Proxy(clientId + 1000 * groupId, lcaConfig, null);
    }


    @Override
    public void run() {
        Random r = new Random();
        Request req = new Request();
        long startTime = System.nanoTime(), now;
        long elapsed = 0, delta = 0, usLat = startTime;
        byte[] response;
        int[] destination = new int[msgDestination.length];

        for (int i = 0; i < msgDestination.length; i++) {
            destination[i] = Integer.parseInt(msgDestination[i]);
        }

        int j = 0;
        while (elapsed / 1e9 < runTime) {
            try {
//                Integer key = r.nextInt(Integer.MAX_VALUE);
                Integer key = j;
                req.setValue(randomString(size).getBytes());
                req.setDestination(destination);
                req.setKey(key);
//                req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
//                req.setType(RequestType.PUT);
                req.setType(RequestType.GET);
                req.setUniqueId(clientId + "-" +  j + "-" + generateUniqueId());
                j++;
                req.setIsRealClient(true);
                response = proxy.atomicMulticast(req);
                if (response == null && req.getType() == RequestType.SIZE) {
                    logger.info("Problem");
                    break;
                }

                now = System.nanoTime();

                if (singleRequest) {
                    System.out.println("RESPONSE: " + (response == null ? "NULL" : new String(response)));
                    break;
                }

                elapsed = (now - startTime);

                localStats.store((now - usLat) / 1000);

                usLat = now;
                if (verbose && elapsed - delta >= 2 * 1e9) {
                    logger.info("Client " + clientId + " ops/second:" + (localStats.getPartialCount() + globalStats.getPartialCount()) / ((float) (elapsed - delta) / 1e9));
                    delta = elapsed;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String targetGroups = String.join("_", msgDestination);
        if (localStats.getCount() > 0) {
            localStats.persist("localStats-client-lca" + groupId + "-targetGroups" + targetGroups + "-" + clientId + ".txt", 15);
            System.out.println("LOCAL STATS:" + localStats);
        }

        if (globalStats.getCount() > 0) {
            globalStats.persist("globalStats-client-lca" + groupId + "-" + clientId + ".txt", 15);
            System.out.println("\nGLOBAL STATS:" + globalStats);
        }

    }

    String randomString(int len) {
        char[] buf = new char[len];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

    String generateUniqueId() {
        return UUID.randomUUID().toString();
    }
}
