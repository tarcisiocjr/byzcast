package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.ProxyIf;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.Stats;

import java.util.Random;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class ClientThread implements Runnable {

    private final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private int clientId;
    private int groupId;
    private int numOfGroups;
    private String[] localConfigs;
    private String globalConfig;
    private boolean verbose;
    private int runTime;
    private int size;
    private Random random;
    private Stats localStats, globalStats;
    private ProxyIf proxy;


    public ClientThread(int clientId, int groupId, String globalConfig, String[] localConfigs, boolean verbose, int runTime, int valueSize) {
        this.clientId = clientId;
        this.groupId = groupId;
        this.numOfGroups = localConfigs == null ? 1 : localConfigs.length;
        this.localConfigs = localConfigs;
        this.globalConfig = globalConfig;
        this.verbose = verbose;
        this.runTime = runTime;
        this.size = valueSize;
        this.random = new Random();
        this.localStats = new Stats();
        this.globalStats = new Stats();
        this.proxy = new Proxy(clientId + 1000 * groupId, globalConfig, localConfigs);
    }


    @Override
    public void run() {
        Random r = new Random();
        Request req = new Request();
        long startTime = System.nanoTime(), now;
        long elapsed = 0, delta = 0, usLat = startTime;
        byte[] response;

        req.setValue(randomString(size).getBytes());
        while (elapsed / 1e9 < runTime) {
            try {
                req.setDestination(r.nextInt() % 2 == 0 || numOfGroups == 1 ? new int[]{groupId} : new int[]{groupId, (groupId + 1) % numOfGroups});
                req.setKey(r.nextInt());
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
                    System.out.println("Client " + clientId + " ops/second:" + localStats.getPartialCount() / ((float) (elapsed - delta) / 1e9));
                    delta = elapsed;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        localStats.persist("localStats-client-" + clientId + ".txt", 5);
        globalStats.persist("globalStats-client-" + clientId + ".txt", 5);

        System.out.println("LOCAL STATS:" + localStats);
        System.out.println("\nGLOBAL STATS:" + globalStats);


    }

    private String randomString(int len) {
        char[] buf = new char[len];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}