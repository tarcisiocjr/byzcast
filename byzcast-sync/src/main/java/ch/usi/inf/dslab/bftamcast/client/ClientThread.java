package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.Stats;

import java.util.Random;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class ClientThread implements Runnable {

    private char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private int clientId;
    private int groupId;
    private int numOfGroups;
    private boolean verbose;
    private int runTime;
    private int size;
    private int globalPerc;
    private Random random;
    private Stats stats;

    private final String[] msgDestination;
    private ProxyIf proxy;


    ClientThread(int clientId, int groupId, String[] globalConfigs, String[] localConfigs, boolean verbose, int runTime, int valueSize, int globalPerc, boolean ng, String[] msgDestination) {
        this.clientId = clientId;
        this.groupId = groupId;
        this.numOfGroups = (localConfigs == null || localConfigs.length == 0) ? 1 : localConfigs.length;
        this.verbose = verbose;
        this.runTime = runTime;
        this.size = valueSize;
        this.globalPerc = globalPerc;
        this.random = new Random();
        this.stats = new Stats();
        this.msgDestination = msgDestination;
        this.proxy = ng ?
                new Proxy(clientId + 1000 * groupId, globalConfigs, null) :
                new Proxy(clientId + 1000 * groupId, globalConfigs, localConfigs);
    }


    @Override
    public void run() {
        Random r = new Random();
        Request req = new Request();
        long startTime = System.nanoTime(), now;
        long elapsed = 0, delta = 0, usLat = startTime;
        byte[] response;
        int[] local = new int[]{groupId};
//        long s = 0, sum = 0;

        int[] destination = new int[msgDestination.length];

        for (int i = 0; i < msgDestination.length; i++) {
            destination[i] = Integer.parseInt(msgDestination[i]);
        }

        req.setValue(randomString(size).getBytes());
        while (elapsed / 1e9 < runTime) {
//            startTime = System.nanoTime();
            try {
                Integer key = r.nextInt(Integer.MAX_VALUE);
                req.setKey(key);
                req.setDestination(destination);
//                req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
                req.setType(RequestType.GET);

//                elapsed = System.nanoTime() - startTime;
//                sum += elapsed; s++;
//                System.out.println(" Tempo antes de response: " + elapsed + " ns" + " - " + sum/s + "ns");

                response = proxy.atomicMulticast(req);
                if (response == null && req.getType() == RequestType.SIZE) {
                    System.err.println("Problem");
                    break;
                }

                now = System.nanoTime();
                elapsed = (now - startTime);
                stats.store((now - usLat) / 1000, req.getDestination().length > 1);

                usLat = now;
                if (verbose && elapsed - delta >= 2 * 1e9) {
                    System.out.println("Client " + clientId + " ops/second:" + (stats.getPartialCount() / ((float) (elapsed - delta) / 1e9)));
                    delta = elapsed;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String targetGroups = String.join("_", msgDestination);
        if (stats.getCount() > 0) {
//            stats.persist("stats-client-g" + groupId + "-" + clientId + ".txt", 15);
            stats.persist("localStats-client-lca" + groupId + "-targetGroups" + targetGroups + "-" + clientId + ".txt", 15);
            System.out.println("LOCAL STATS:" + stats);
        }
//        for (int i = 0; i < count.length; i++) {
//            System.out.println("msg to (" + allDests[i][0] + ", " + allDests[i][1] + ": " + count[i]);
//        }

    }

    private String randomString(int len) {
        char[] buf = new char[len];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}
