package ch.usi.inf.dslab.bftamcast.client;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import ch.usi.inf.dslab.bftamcast.RequestIf;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public interface ProxyIf {
	//change to void for async and call callback to client
//    public byte[] reliableMulticast(RequestIf req);
//
//    public byte[] atomicMulticast(RequestIf req);
    
    public AsynchServiceProxy asyncAtomicMulticast(RequestIf req,ReplyListener listener);
}
