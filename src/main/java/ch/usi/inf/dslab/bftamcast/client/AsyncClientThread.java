package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;

import java.util.Random;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class AsyncClientThread extends ClientThread implements AsyncProxyListenerIf {

    Request req;
    private int outstanding;
    private Random r;
    private int[] all, local;
    private int replies = 0;
    private long startTime, now;
    private long elapsed = 0, delta = 0, usLat = startTime;
    private byte[] response;

    public AsyncClientThread(int clientId, int groupId, String globalConfig, String[] localConfigs,
                             boolean verbose, int runTime, int valueSize, int globalPerc, int outstanding, boolean ng) {
        super(clientId, groupId, globalConfig, localConfigs, verbose, runTime, valueSize, globalPerc, ng);
        this.outstanding = outstanding;
        r = new Random();
        all = new int[numOfGroups];
        local = new int[]{groupId};

        for (int i = 0; i < numOfGroups; i++)
            all[i] = i;

        this.proxy = ng ?
                new AsyncProxy(clientId + 1000 * groupId, globalConfig, null) :
                new AsyncProxy(clientId + 1000 * groupId, globalConfig, localConfigs);
    }

    @Override
    public void run() {
        req = new Request();
        startTime = System.nanoTime();
        req.setValue(randomString(size).getBytes());
        for (int i = 0; i < outstanding; i++) {
            setRequest(req);
            proxy.asyncAtomicMulticast(req, this);
        }
    }


    private void setRequest(Request req) {
        req.setDestination(r.nextInt(100) >= globalPerc || numOfGroups == 1 ? local : all);
        req.setKey(r.nextInt(Integer.MAX_VALUE));
        req.setType(req.getDestination().length > 1 ? RequestType.SIZE : RequestType.PUT);
    }

    @Override
    public void receiveResponse(byte[] response) {
        this.response = response;
        if (elapsed / 1e9 < runTime) {
            try {
                if (response == null && req.getType() == RequestType.SIZE) {
                    System.err.println("Problem");
                } else {
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
                    setRequest(req);
                    proxy.asyncAtomicMulticast(req, this);
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
        ((AsyncProxy) proxy).close();
    }
}
