package ch.usi.inf.dslab.byzcast.client;

import ch.usi.inf.dslab.byzcast.RequestIf;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */
public interface ProxyIf {
    public byte[] reliableMulticast(RequestIf req);

    public byte[] atomicMulticast(RequestIf req);
}
