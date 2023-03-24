package ch.usi.inf.dslab.byzcast;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */
public interface RequestIf {
    public byte[] getValue();

    public void setValue(byte[] value);

    public int[] getDestination();

    public void setDestination(int[] destination);

    public byte[] toBytes();

    public void fromBytes(byte[] b);
}
