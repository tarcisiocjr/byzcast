package ch.usi.inf.dslab.bftamcast;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public interface ProxyIf {
    public byte[] reliableMulticast(RequestIf req);
    public byte[] atomicMulticast(RequestIf req);
}
