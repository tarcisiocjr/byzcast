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
    private Stats localStats, globalStats;
    private ProxyIf proxy;


    ClientThread(int clientId, int groupId, String[] globalConfigs, String[] localConfigs, boolean verbose, int runTime, int valueSize, int globalPerc, boolean ng) {
        this.clientId = clientId;
        this.groupId = groupId;
        this.numOfGroups = (localConfigs == null || localConfigs.length == 0) ? 1 : localConfigs.length;
        this.verbose = verbose;
        this.runTime = runTime;
        this.size = valueSize;
        this.globalPerc = globalPerc;
        this.random = new Random();
        this.localStats = new Stats();
        this.globalStats = new Stats();
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

        /*
        int[] all = new int[numOfGroups];
        for (int i = 0; i < numOfGroups; i++)
            all[i] = i;
        */
        int[][] allDests = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};
        int[] percentage = {40, 45, 50, 55, 60, 101};

        req.setValue(randomString(size).getBytes());
        while (elapsed / 1e9 < runTime) {
            try {
                if (r.nextInt(100) >= globalPerc || numOfGroups == 1) {
                    req.setDestination(local);
                } else {
                    int index = 0, rn = r.nextInt(100);
                    while (rn > percentage[index]) index++;

                    req.setDestination(allDests[index]);
                }
                req.setKey(r.nextInt(Integer.MAX_VALUE));
                req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
                response = proxy.atomicMulticast(req);
                if (response == null && req.getType() == RequestType.SIZE) {
                    System.err.println("Problem");
                    break;
                }
                now = System.nanoTime();
                elapsed = (now - startTime);

                if (req.getDestination().length > 1)
                    globalStats.store((now - usLat) / 1000);
                else
                    localStats.store((now - usLat) / 1000);

                usLat = now;
                if (verbose && elapsed - delta >= 2 * 1e9) {
                    System.out.println("Client " + clientId + " ops/second:" + (localStats.getPartialCount() + globalStats.getPartialCount()) / ((float) (elapsed - delta) / 1e9));
                    delta = elapsed;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (localStats.getCount() > 0) {
            localStats.persist("localStats-client-g" + groupId + "-" + clientId + ".txt", 15);
            System.out.println("LOCAL STATS:" + localStats);
        }

        if (globalStats.getCount() > 0) {
            globalStats.persist("globalStats-client-g" + groupId + "-" + clientId + ".txt", 15);
            System.out.println("\nGLOBAL STATS:" + globalStats);
        }
    }

    private String randomString(int len) {
        char[] buf = new char[len];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}
