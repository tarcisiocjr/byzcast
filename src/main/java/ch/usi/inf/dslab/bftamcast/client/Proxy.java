package ch.usi.inf.dslab.bftamcast.client;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.RequestIf;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Proxy implements ProxyIf {

    private int proxyId;

    //change from Service proxy to async
    private AsynchServiceProxy[] localClients;
    private AsynchServiceProxy globalClient;

    public Proxy(int clientId, String globalConfigPath, String[] localConfigPaths) {
        proxyId = clientId;
        globalClient = new AsynchServiceProxy(proxyId, globalConfigPath);

        if (localConfigPaths != null) {
            localClients = new AsynchServiceProxy[localConfigPaths.length];

            for (int i = 0; i < localConfigPaths.length; i++)
                localClients[i] = new AsynchServiceProxy(clientId, localConfigPaths[i]);
        }
    }

//    @Override
//    public byte[] reliableMulticast(RequestIf req) {
//        byte[] response;
//
//        if (req.getDestination().length > 1 || localClients == null)
//            response = globalClient.invokeUnordered(req.toBytes());
//        else
//            response = localClients[req.getDestination()[0]].invokeUnordered(req.toBytes());
//
//        req.fromBytes(response);
//        return req.getValue();
//    }

//    @Override
//    public byte[] atomicMulticast(RequestIf req) {
//        byte[] response;
//        if (req.getDestination().length > 1 || localClients == null)
//            response = globalClient.invokeOrdered(req.toBytes());
//        else
//            response = localClients[req.getDestination()[0]].invokeOrdered(req.toBytes());
//
//        req.fromBytes(response);
//        return req.getValue();
//    }

	@Override
	public AsynchServiceProxy asyncAtomicMulticast(RequestIf req, ReplyListener listener) {
		AsynchServiceProxy proxy = null;
		if(req.getDestination().length >1 || localClients == null) {
			proxy = globalClient;
		}else {
			proxy = localClients[req.getDestination()[0]];
		}
		proxy.invokeAsynchRequest(req.toBytes(), listener, TOMMessageType.ORDERED_REQUEST);
		return proxy;
	}
}
