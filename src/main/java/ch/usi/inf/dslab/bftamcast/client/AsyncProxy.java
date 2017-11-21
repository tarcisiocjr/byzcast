package ch.usi.inf.dslab.bftamcast.client;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.RequestIf;

import java.util.Arrays;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class AsyncProxy implements ProxyIf {
    private AsynchServiceProxy[] localClients;
    private AsynchServiceProxy globalClient;

    public AsyncProxy(int proxyId, String globalConfigPath, String[] localConfigPaths) {
        globalClient = new AsynchServiceProxy(proxyId, globalConfigPath);
        if (localConfigPaths != null) {
            localClients = new AsynchServiceProxy[localConfigPaths.length];
            for (int i = 0; i < localConfigPaths.length; i++)
                localClients[i] = new AsynchServiceProxy(proxyId, localConfigPaths[i]);
        }
    }


    public byte[] reliableMulticast(RequestIf req) {
        throw new UnsupportedOperationException("Not implemented by ASYNC proxy");
    }

    public byte[] atomicMulticast(RequestIf req) {
        throw new UnsupportedOperationException("Not implemented by ASYNC proxy");
    }

    @Override
    public byte[] asyncReliableMulticast(RequestIf req, AsyncProxyListenerIf listener) {
        AsynchServiceProxy proxy = getProxy(req);
        proxy.invokeAsynchRequest(req.toBytes(), new ProxyListener(proxy, listener), TOMMessageType.UNORDERED_REQUEST);
        return null;
    }

    @Override
    public byte[] asyncAtomicMulticast(RequestIf req, AsyncProxyListenerIf listener) {
        AsynchServiceProxy proxy = getProxy(req);
        proxy.invokeAsynchRequest(req.toBytes(), new ProxyListener(proxy, listener), TOMMessageType.ORDERED_REQUEST);
        return null;
    }

    private AsynchServiceProxy getProxy(RequestIf req) {
        return (req.getDestination().length > 1 || localClients == null) ?
                globalClient :
                localClients[req.getDestination()[0]];
    }

    public void close() {
        globalClient.close();
        if (localClients != null) {
            for (int i = 0; i < localClients.length; i++)
                localClients[i].close();
        }
    }

    class ProxyListener implements ReplyListener {
        private int replies = 0;
        private AsyncProxyListenerIf listener;
        private AsynchServiceProxy proxy;

        ProxyListener(AsynchServiceProxy proxy, AsyncProxyListenerIf listener) {
            this.proxy = proxy;
            this.listener = listener;
        }

        @Override
        public void reset() {
            replies = 0;
        }

        @Override
        public void replyReceived(RequestContext context, TOMMessage reply) {
            replies++;
            double q = Math.ceil((double) (proxy.getViewManager().getCurrentViewN() + proxy.getViewManager().getCurrentViewF() + 1) / 2.0);
            if (replies == q) {
                proxy.cleanAsynchRequest(context.getOperationId());
                listener.receiveResponse(reply.getContent());
            }
        }

    }
}
