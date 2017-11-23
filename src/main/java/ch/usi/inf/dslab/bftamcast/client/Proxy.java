package ch.usi.inf.dslab.bftamcast.client;

import bftsmart.tom.ServiceProxy;
import ch.usi.inf.dslab.bftamcast.RequestIf;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Proxy implements ProxyIf {

    private int proxyId;

    private ServiceProxy[] localClients;
    private ServiceProxy globalClient;

    public Proxy(int clientId, String globalConfigPath, String[] localConfigPaths) {
        proxyId = clientId;
        globalClient = new ServiceProxy(proxyId, globalConfigPath);

        if (localConfigPaths != null) {
            localClients = new ServiceProxy[localConfigPaths.length];

            for (int i = 0; i < localConfigPaths.length; i++)
                localClients[i] = new ServiceProxy(clientId, localConfigPaths[i]);
        }
    }

    @Override
    public byte[] reliableMulticast(RequestIf req) {
        byte[] response;

        if (req.getDestination().length > 1 || localClients == null)
            response = globalClient.invokeUnordered(req.toBytes());
        else
            response = localClients[req.getDestination()[0]].invokeUnordered(req.toBytes());

        req.fromBytes(response);
        return req.getValue();
    }

    @Override
    public byte[] atomicMulticast(RequestIf req) {
        byte[] response;
        if (req.getDestination().length > 1 || localClients == null)
            response = globalClient.invokeOrdered(req.toBytes());
        else
            response = localClients[req.getDestination()[0]].invokeOrdered(req.toBytes());

        req.fromBytes(response);
        return req.getValue();
    }
}
