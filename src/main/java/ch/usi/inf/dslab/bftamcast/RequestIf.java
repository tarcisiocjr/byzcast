package ch.usi.inf.dslab.bftamcast;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
//TODO clean up and modify to keep add needed method to make sure any request will work
public interface RequestIf {
    public byte[] getValue();

//    public void setValue(byte[] value);

    public int[] getDestination();

//    public void setDestination(int[] destination);

    public byte[] toBytes();

    public void fromBytes(byte[] b);
}
