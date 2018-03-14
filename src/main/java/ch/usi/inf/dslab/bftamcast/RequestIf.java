package ch.usi.inf.dslab.bftamcast;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public interface RequestIf {
    public byte[] getValue();

//    public void setValue(byte[] value);

    public int[] getDestination();

//    public void setDestination(int[] destination);

    public byte[] toBytes();

    public void fromBytes(byte[] b);
}
