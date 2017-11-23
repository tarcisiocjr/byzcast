package ch.usi.inf.dslab.bftamcast.kvs;

import ch.usi.inf.dslab.bftamcast.RequestIf;

import java.io.*;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Request implements RequestIf, Serializable {
    private RequestType type;
    private int key;
    private byte[] value;
    private int[] destination;
    private int seqNumber;

    public Request() {
        this(RequestType.NOP, -1, null, null);
    }

    public Request(RequestType type, int key, byte[] value, int[] destination) {
        this(type, key, value, destination, 0);
    }

    public Request(RequestType type, int key, byte[] value, int[] destination, int seqNumber) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.destination = destination;
        this.seqNumber = seqNumber;
    }

    public static byte[] ArrayToBytes(Request[] reqs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            ObjectOutputStream dos = new ObjectOutputStream(out);
            dos.writeInt(reqs.length);
            for (Request r :
                    reqs) {
                dos.writeObject(r);
            }
            dos.close();
            out.close();
        } catch (IOException e) {
            System.err.println("Unable to convert RequestIf to bytes");
            e.printStackTrace();
            return null;
        }
        return out.toByteArray();
    }

    public static Request[] ArrayfromBytes(byte[] b) {
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        Request[] reqs = null;
        int size;

        try {
            ObjectInputStream dis = new ObjectInputStream(in);
            size = dis.readInt();
            reqs = new Request[size];
            for (int i = 0; i < reqs.length; i++) reqs[i] = (Request) dis.readObject();


        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Unable to convert bytes to Request");
            e.printStackTrace();
        }
        return reqs;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public int[] getDestination() {
        return destination;
    }

    public void setDestination(int[] destination) {
        this.destination = destination;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        try {
            dos.writeUTF(this.type.name());
            dos.writeInt(this.key);
            dos.writeInt(this.value == null ? 0 : this.value.length);
            if (value != null)
                dos.write(this.value);
            dos.writeInt(this.seqNumber);
            dos.writeInt(this.destination.length);
            for (int dest : destination)
                dos.writeInt(dest);
        } catch (IOException e) {
            System.err.println("Unable to convert RequestIf to bytes");
            e.printStackTrace();
            return null;
        }
        return out.toByteArray();
    }

    public void fromBytes(byte[] b) {
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(in);
        int destSize, vSize;

        try {
            this.type = RequestType.valueOf(dis.readUTF());
            this.key = dis.readInt();
            vSize = dis.readInt();
            if (vSize > 0) {
                this.value = new byte[vSize];
                dis.read(this.value);
            } else
                this.value = null;
            this.seqNumber = dis.readInt();
            destSize = dis.readInt();
            this.destination = new int[destSize];
            for (int i = 0; i < destSize; i++) {
                this.destination[i] = dis.readInt();
            }
        } catch (IOException e) {
            System.err.println("Unable to convert bytes to RequestIf");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("RequestIf '" + this.type + "', key " + this.key + ", seq. number" + this.seqNumber + " to groups ( ");
        for (int dest : this.destination)
            buf.append(dest + " ");
        buf.append(')');
        return buf.toString();
    }

}
