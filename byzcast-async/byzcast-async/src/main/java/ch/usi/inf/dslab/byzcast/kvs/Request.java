package ch.usi.inf.dslab.byzcast.kvs;

import ch.usi.inf.dslab.byzcast.RequestIf;

import java.io.*;
import java.util.Vector;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */

/**
 * type: tipo da requisição
 * status: status da requisicao
 * key: key
 * value: value
 * prcsReq: conjunto de processos dos quais recebeu a mensagem e deve responder
 * destination: id do grupo de destino
 * group: usado na implementacao original para controle
 * uniqueId: usado na nova implementação
 * */
public class Request implements RequestIf, Serializable {
    private RequestType type;
    private RequestStatus status;
    private int key;
    private byte[] value;
    private int prcsReq;
    private int[] destination;
    private int group;
    private String uniqueId;
    private boolean realClient;
    private int seqNumber;

    public Request() {
        this(RequestType.NOP, RequestStatus.NDA ,-1, null, -1, null, null, false);
    }

    public Request(RequestType type, RequestStatus status, int key, byte[] value, int prcsReq, String uniqueId, int[] destination, boolean realClient) {
        this(type, status, key, value, prcsReq, destination, 0, null, false);
    }

    public Request(RequestType type, RequestStatus status, int key, byte[] value, int prcsReq, int[] destination, int group, String uniqueId, boolean realClient) {
        this.type = type;
        this.status = status;
        this.key = key;
        this.value = value;
        this.prcsReq = prcsReq;
        this.destination = destination;
        this.group = group;
        this.uniqueId = uniqueId;
        this.realClient = realClient;
    }

    public Request(byte[] content) {
        fromBytes(content);
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

    public static byte[] VectorToBytes(Vector<Request> reqs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            ObjectOutputStream dos = new ObjectOutputStream(out);
            dos.writeInt(reqs.size());
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


    public static Vector<Request> VectorfromBytes(byte[] b) {
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        Vector<Request> reqs = null;
        int size;

        try {
            ObjectInputStream dis = new ObjectInputStream(in);
            size = dis.readInt();
            reqs = new Vector<Request>(size);
            for (int i = 0; i < size; i++) reqs.add((Request) dis.readObject());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Unable to convert bytes to Request");
            e.printStackTrace();
        }
        return reqs;
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

    public RequestStatus getStatus() { return status; }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
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

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public void setStatus(RequestStatus status) { this.status = status; }

    public void setIsRealClient(boolean realClient) { this.realClient = realClient; }

    public boolean getIsRealClient() { return realClient; }

    public void setPrcsReq(int prcsReq) {this.prcsReq = prcsReq;}

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        try {
            dos.writeUTF(this.type.name());
            dos.writeUTF(this.status.name());
            dos.writeInt(this.key);
            dos.writeInt(this.value == null ? 0 : this.value.length);
            if (value != null)
                dos.write(this.value);
            dos.writeInt(this.group);
            dos.writeInt(this.prcsReq);
            dos.writeUTF(this.uniqueId);
            dos.writeBoolean(this.realClient);
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
            this.status = RequestStatus.valueOf(dis.readUTF());
            this.key = dis.readInt();
            vSize = dis.readInt();
            if (vSize > 0) {
                this.value = new byte[vSize];
                dis.read(this.value);
            } else
                this.value = null;
            this.group = dis.readInt();
            this.prcsReq = dis.readInt();
            this.uniqueId = dis.readUTF();
            this.realClient = dis.readBoolean();
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
        buf.append("RequestIf '" + this.type + " status '" + this.status + "', key " + this.key + ", UUID " + this.uniqueId + ", isRealClient " + this.realClient + ", group " + this.group + ", prcsReq " + this.prcsReq + " to groups ( ");
        for (int dest : this.destination)
            buf.append(dest + " ");
        buf.append(')');
        return buf.toString();
    }

}
