package ch.usi.inf.dslab.bftamcast.kvs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import ch.usi.inf.dslab.bftamcast.RequestIf;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Christian Vuerich - christian.vuerich@usi.ch
 */
public class Request implements RequestIf, Serializable {
	/**
	 * TODO remove unused fields
	 */
	private static final long serialVersionUID = -8560523066225958549L;
	private RequestType type;
	private int key;
	private int client;
	private int sender;
	private byte[] value;
	private byte[][][] result;
	private int[] destination;
	private int seqNumber;
	private long destIdentifier;
	private Request[] batch;
	private boolean isBatch = false;

	/**
	 * create a request object from a byte[]
	 * 
	 * @param reqBytes
	 */
	public Request(byte[] reqBytes) {
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
	public Request(RequestType type, int key, byte[] value, int[] destination, int seqNumber, int client, int sender,
			long destIdentifier) {
		this.client = client;
		this.sender = sender;
		this.type = type;
		this.key = key;
		this.value = value;
		this.destination = destination;
		this.result = new byte[1][destination.length][];
		this.seqNumber = seqNumber;
		this.destIdentifier = destIdentifier;
	}

//	public Request(RequestType type, int key, byte[] value, int[] destination, int seqNumber, int client, int sender,
//			long destIdentifier, Request[] batch) {
////		this.isBatch = true;
//		this.batch = batch;
//		this.client = client;
//		this.sender = sender;
//		this.type = type;
//		this.key = key;
//		this.value = value;
//		this.destination = destination;
//		this.result = new byte[batch.length][destination.length][];
//		this.destinationhandled = new int[destination.length];
//		for (int i = 0; i < destinationhandled.length; i++) {
//			destinationhandled[i] = -1;
//		}
//		this.seqNumber = seqNumber;
//		this.destIdentifier = destIdentifier;
//	}

	public long getDestIdentifier() {
		return destIdentifier;
	}

	/**
	 * convert an array of requests to byte[]
	 * 
	 * @param reqs
	 * @return
	 */

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

	/**
	 * convert an array of byte to an array of requests
	 * 
	 * @param b
	 * @return
	 */
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

	/**
	 * getter for Type field
	 * 
	 * @return
	 */
	public RequestType getType() {
		return type;
	}

	/**
	 * set the result for a groupiD, at the same index of the groupID in
	 * destinations
	 * 
	 * @param eval
	 * @param groupID
	 */
	public void setResult(byte[] eval, int groupID) {
		// look for index in destinations of groupID
		for (int i = 0; i < destination.length; i++) {
			if (destination[i] == groupID) {
				// set result
				this.result[0][i] = eval;
			}
		}
	}

	public void mergeReplies(byte[][] replies) {
		if (replies.length != result.length) {
			System.err.println("Error merging results");
		}
		for (int i = 0; i < replies.length; i++) {
			if (replies[i] != null) {
				this.result[0][i] = replies[i];
			}
		}
	}

	/**
	 * return the result byte[] at the same index groupID has in destinations
	 * 
	 * @param groupID
	 * @return
	 */
	public byte[] getGroupResult(int groupID) {
		for (int i = 0; i < destination.length; i++) {
			if (destination[i] == groupID) {
				return result[0][i];
			}
		}
		return null;
	}

	/**
	 * Getter for key field
	 * 
	 * @return
	 */
	public int getKey() {
		return key;
	}

	/**
	 * Getter for value field
	 * 
	 * @return
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * Getter for value result
	 * 
	 * @return
	 */
	public byte[][][] getResult() {
		return result;
	}

	/**
	 * Getter for value destination
	 * 
	 * @return
	 */
	public int[] getDestination() {
		return destination;
	}

	
	/**
	 * Getter for sequence number field
	 * 
	 * @return
	 */
	public int getSeqNumber() {
		return seqNumber;
	}

	/**
	 * Getter for clientID field
	 * 
	 * @return
	 */
	public int getClient() {
		return client;
	}

	/**
	 * Getter for sender field
	 * 
	 * @return
	 */
	public int getSender() {
		return sender;
	}

	/**
	 * Setter for sender field
	 * 
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
			System.out.println("asfasdf   " + this.type.ordinal());

			// dos.writeUTF(this.type.name());
			dos.writeInt(this.type.ordinal());
			dos.writeInt(this.key);
			dos.writeLong(this.destIdentifier);
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
				for (byte[][] groupRes : result) {
					dos.writeInt(groupRes == null ? 0 : groupRes.length);
					if (groupRes != null) {
						for (byte[] groupResres : groupRes) {
							dos.writeInt(groupResres == null ? 0 : groupResres.length);
							if (groupResres != null)
								dos.write(groupResres);
						}
					}

				}
			}
			dos.writeBoolean(isBatch);
			System.out.println("isbatch = " + isBatch);
			if (isBatch) {
				dos.writeInt(this.batch == null ? 0 : this.batch.length);
				if (batch != null) {
					for (Request r : batch) {
						byte[] reqbytes = r.toBytes();
						dos.writeInt(reqbytes == null ? 0 : reqbytes.length);
						if (reqbytes != null)
							dos.write(reqbytes);
					}
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
		int destSize, vSize, resultSize, groupResultSize, groupResultSize2;

		try {
			int a = dis.readInt();
			System.out.println("asfasddsafooof   " + a);
			this.type = RequestType.values()[a];
			this.key = dis.readInt();
			this.destIdentifier = dis.readLong();
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
				this.result = new byte[resultSize][][];
				for (int i = 0; i < resultSize; i++) {
					groupResultSize = dis.readInt();
					if (groupResultSize > 0) {
						this.result[i] = new byte[groupResultSize][];
						for (int ii = 0; ii < groupResultSize; ii++) {
							groupResultSize2 = dis.readInt();
							if (groupResultSize2 > 0) {
								this.result[i][ii] = new byte[groupResultSize2];
								dis.read(this.result[i][ii]);
							}
						}
					}
				}
			}
			this.isBatch = dis.readBoolean();
			System.out.println("isbatch = " + isBatch);
			if (isBatch) {
				System.out.println("BAAAATCH ");
				resultSize = dis.readInt();
				if (resultSize > 0) {
					this.batch = new Request[resultSize];
					for (int i = 0; i < resultSize; i++) {
						groupResultSize = dis.readInt();
						if (groupResultSize > 0) {
							byte[] tmp = new byte[groupResultSize];
							dis.read(tmp);
							batch[i] = new Request(tmp);
						}
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
		buf.append("RequestIf '" + this.type + "', key " + this.key + ", seq. number" + this.seqNumber + ", client "
				+ this.client + " to groups ( ");
		for (int dest : this.destination)
			buf.append(dest + " ");
		buf.append(')');
		return buf.toString();
	}

	/**
	 * compare a input request to this object, return true if all content is the
	 * same
	 * 
	 * @param r
	 * @return
	 */
	// public boolean equals(Request r) {
	//// System.out.println("res = " + Arrays.equals(this.getResult(),
	// r.getResult()));
	//// System.out.println("res = " + this.getResult().equals(r.getResult()));
	//// System.out.println();
	//
	//// System.out.println(this.key == r.key && this.type == r.type &&
	// this.seqNumber == r.seqNumber && this.client == r.client &&
	//// this.lcaID == r.lcaID && Arrays.equals(this.getValue(), r.getValue()) &&
	// Arrays.equals(this.getDestination(), r.getDestination())
	//// && Arrays.equals(this.getResult(), r.getResult()));
	////
	//// return (this.key == r.key && this.type == r.type && this.seqNumber ==
	// r.seqNumber && this.client == r.client &&
	//// this.lcaID == r.lcaID && Arrays.equals(this.getValue(), r.getValue()) &&
	// Arrays.equals(this.getDestination(), r.getDestination())
	//// && Arrays.equals(this.getResult(), r.getResult()));
	// if (this.key != r.key) {
	// System.out.println("key problem");
	// return false;
	// }
	// if (this.type != r.type) {
	// System.out.println("type problem");
	// return false;
	// }
	// if (this.seqNumber != r.seqNumber) {
	// System.out.println("seq problem");
	// return false;
	// }
	// if (this.client != r.client) {
	// System.out.println("sender problem");
	// return false;
	// }
	// if(!Arrays.equals(this.getValue(), r.getValue())){
	// return false;
	// }
	// if(!Arrays.equals(this.getDestination(), r.getDestination())){
	// return false;
	// }
	//
	// if (result == null && r.result != null) {
	// return false;
	// }
	// if (r.result == null && result != null) {
	// return false;
	// }
	// if (result != null) {
	//
	// for (int i = 0; i < result.length; i++) {
	// if (result[i] == null && r.result[i] != null) {
	// return false;
	// }
	// if (r.result[i] == null && result[i] != null) {
	// return false;
	// }
	// if (result[i] != null && !Arrays.equals(result[i], r.result[i])) {
	// return false;
	// }
	// }
	//
	// }
	//
	// return true;
	// }

}
