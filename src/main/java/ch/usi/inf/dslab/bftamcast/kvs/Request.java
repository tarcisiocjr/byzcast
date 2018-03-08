package ch.usi.inf.dslab.bftamcast.kvs;

import ch.usi.inf.dslab.bftamcast.RequestIf;

import java.io.*;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Request implements RequestIf, Serializable {
	private RequestType type;
	private int key;
	private int sender;
	private byte[] value;
	private int[] destination;
	private int seqNumber;

	public Request() {
		this(RequestType.NOP, -1, null, null, -1, -1);
	}

	public Request(RequestType type, int key, byte[] value, int[] destination, int seqNumber, int sender) {
		this.sender = sender;
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
			for (Request r : reqs) {
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
			for (int i = 0; i < reqs.length; i++)
				reqs[i] = (Request) dis.readObject();

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

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
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
			dos.writeInt(sender);
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
			this.sender = dis.readInt();
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
		buf.append("RequestIf '" + this.type + "', key " + this.key + ", seq. number" + this.seqNumber + ", sender "
				+ this.sender + " to groups ( ");
		for (int dest : this.destination)
			buf.append(dest + " ");
		buf.append(')');
		return buf.toString();
	}

	public boolean equals(Request r) {
		if (this.key != r.key)
			return false;
		if (this.type != r.type)
			return false;
		if (this.seqNumber != r.seqNumber)
			return false;
		if (this.sender != r.sender)
			return false;

		if (this.getValue() == null) {
			if (r.getValue() != null)
				return false;
		} else {
			if (r.getValue() == null)
				return false;
		}

		if (this.getDestination() == null) {
			if (r.getDestination() != null)
				return false;
		} else {
			if (r.getDestination() == null)
				return false;
		}
		if (value != null) {
			if (this.value.length != r.getValue().length)
				return false;
			for (int i = 0; i < value.length; i++) {
				if (value[i] != r.getValue()[i])
					return false;
			}
		}
		if (destination != null) {

			if (this.destination.length != r.getDestination().length)
				return false;
			for (int i = 0; i < destination.length; i++) {
				if (destination[i] != r.getDestination()[i])
					return false;
			}
		}

		return true;
	}

}
