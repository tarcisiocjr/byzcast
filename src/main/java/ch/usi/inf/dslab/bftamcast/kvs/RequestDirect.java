package ch.usi.inf.dslab.bftamcast.kvs;

import ch.usi.inf.dslab.bftamcast.RequestIf;

import java.io.*;
import java.util.Arrays;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */
public class RequestDirect implements RequestIf, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8560523066225958549L;
	private RequestType type;
	private int key;
	private int client;
	private int sender;
	private byte[] value;
	private byte[][] result;
	private int[] destination;
	private int seqNumber;


	/**
	 * create a request object from a byte[]
	 * 
	 * @param reqBytes
	 */
	public RequestDirect(byte[] reqBytes) {
		fromBytes(reqBytes);
	}

	/**
	 * 
	 * @param type
	 * @param key
	 * @param value
	 * @param destination
	 * @param seqNumber
	 * @param sender
	 */
	public RequestDirect(RequestType type, int key, byte[] value, int[] destination, int seqNumber, int client, int sender) {
		this.client = client;
		this.sender = sender;
		this.type = type;
		this.key = key;
		this.value = value;
		this.destination = destination;
		this.result = new byte[destination.length][];
		this.seqNumber = seqNumber;
	}

	/**
	 * convert an array of requests to byte[]
	 * 
	 * @param reqs
	 * @return
	 */
	public static byte[] ArrayToBytes(RequestDirect[] reqs) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			ObjectOutputStream dos = new ObjectOutputStream(out);
			dos.writeInt(reqs.length);
			for (RequestDirect r : reqs) {
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

	/**
	 * convert an array of byte to an array of requests
	 * 
	 * @param b
	 * @return
	 */
	public static RequestDirect[] ArrayfromBytes(byte[] b) {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		RequestDirect[] reqs = null;
		int size;

		try {
			ObjectInputStream dis = new ObjectInputStream(in);
			size = dis.readInt();
			reqs = new RequestDirect[size];
			for (int i = 0; i < reqs.length; i++)
				reqs[i] = (RequestDirect) dis.readObject();

		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Unable to convert bytes to Request");
			e.printStackTrace();
		}
		return reqs;
	}

	/**
	 * getter for Type field
	 * @return
	 */
	public RequestType getType() {
		return type;
	}

	/**
	 * set the result for a groupiD, at the same index of the groupID in destinations
	 * @param eval
	 * @param groupID
	 */
	public void setResult(byte[] eval, int groupID) {
		//look for index in destinations of groupID
		for (int i = 0; i < destination.length; i++) {
			if (destination[i] == groupID) {
				//set result
				this.result[i] = eval;
			}
		}
	}
	
	public void  mergeReplies(byte[][] replies) {
		if(replies.length != result.length) {
			System.err.println("Error merging results");
		}
		for (int i = 0; i < replies.length; i++) {
			if(replies[i] != null) {
				result[i] = replies[i];
			}
		}
	}

	/**
	 * return the result byte[] at the same index groupID has in destinations
	 * @param groupID
	 * @return
	 */
	public byte[] getGroupResult(int groupID) {
		for (int i = 0; i < destination.length; i++) {
			if (destination[i] == groupID) {
				return result[i];
			}
		}
		return null;
	}
	
	/**
	 * Getter for key field
	 * @return
	 */
	public int getKey() {
		return key;
	}

	/**
	 * Getter for value field
	 * @return
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * Getter for value result
	 * @return
	 */
	public byte[][] getResult() {
		return result;
	}

	/**
	 * Getter for value destination
	 * @return
	 */
	public int[] getDestination() {
		return destination;
	}

	/**
	 * Getter for sequence number field
	 * @return
	 */
	public int getSeqNumber() {
		return seqNumber;
	}

	/**
	 * Getter for clientID field
	 * @return
	 */
	public int getClient() {
		return client;
	}

	/**
	 * Getter for sender field
	 * @return
	 */
	public int getSender() {
		return sender;
	}

	/**
	 * Setter for sender field
	 * @return
	 */
	public void setSender(int sender) {
		this.sender = sender;
	}

	/**
	 * Convert Request data to byte[]
	 */
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
			dos.writeInt(this.client);
			dos.writeInt(this.sender);
			dos.writeInt(this.destination.length);
			for (int dest : destination)
				dos.writeInt(dest);
			dos.writeInt(this.result == null ? 0 : this.result.length);
			if (result != null) {
				for (byte[] groupRes : result) {
					dos.writeInt(groupRes == null ? 0 : groupRes.length);
					if (groupRes != null)
						dos.write(groupRes);
				}
			}

		} catch (IOException e) {
			System.err.println("Unable to convert RequestIf to bytes");
			e.printStackTrace();
			return null;
		}
		return out.toByteArray();
	}

	/**
	 * fill request object from byte[]representing the data inside the object
	 */
	public void fromBytes(byte[] b) {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		DataInputStream dis = new DataInputStream(in);
		int destSize, vSize, resultSize, groupResultSize;

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
			this.client = dis.readInt();
			this.sender = dis.readInt();
			destSize = dis.readInt();
			this.destination = new int[destSize];
			for (int i = 0; i < destSize; i++) {
				this.destination[i] = dis.readInt();
			}
			resultSize = dis.readInt();
			if (resultSize > 0) {
				this.result = new byte[resultSize][];
				for (int i = 0; i < resultSize; i++) {
					groupResultSize = dis.readInt();
					if (groupResultSize > 0) {
						this.result[i] = new byte[groupResultSize];
						dis.read(this.result[i]);
					}
				}
			}

		} catch (IOException e) {
			System.err.println("Unable to convert bytes to RequestIf");
			e.printStackTrace();
		}
	}

	/**
	 * output the object as a string
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("RequestIf '" + this.type + "', key " + this.key + ", seq. number " + this.seqNumber + ", client "
				+ this.client +", sender " + this.sender + " to groups ( ");
		for (int dest : this.destination)
			buf.append(dest + " ");
		buf.append(')');
		return buf.toString();
	}

	/**
	 * compare a input request to this object, return true if all content is the same
	 * @param r
	 * @return
	 */
	public boolean equals(RequestDirect r) {
		if (this.key != r.key) {
			System.out.println("key problem");
			return false;
		}
		if (this.type != r.type) {
			System.out.println("type problem");
			return false;
		}
		if (this.seqNumber != r.seqNumber) {
			System.out.println("seq problem");
			return false;
		}
		if (this.client != r.client) {
			System.out.println("sender problem");
			return false;
		}

		if (this.getValue() == null) {
			if (r.getValue() != null) {
				System.out.println("value problem");
				return false;
			}
		} else {
			if (r.getValue() == null) {
				System.out.println("value problem1");
				return false;
			}
		}

		if (this.getDestination() == null) {
			if (r.getDestination() != null) {
				System.out.println("destination problem");
				return false;
			}
		} else {
			if (r.getDestination() == null) {
				System.out.println("destination problem1");
				return false;
			}
		}
		if (value != null) {
			if (!Arrays.equals(value, r.value)) {
				return false;
			}
		}
		if (destination != null) {

			if (!Arrays.equals(destination, r.destination)) {
				return false;
			}
		}
		if (result == null && r.result != null) {
			return false;
		}
		if (r.result == null && result != null) {
			return false;
		}
		if (result != null) {

			for (int i = 0; i < result.length; i++) {
				if (result[i] == null && r.result[i] != null) {
					return false;
				}
				if (r.result[i] == null && result[i] != null) {
					return false;
				}
				if (result[i] != null && !Arrays.equals(result[i], r.result[i])) {
					return false;
				}
			}

		}

		return true;
	}

}
