/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.RequestIf;
import ch.usi.inf.dslab.bftamcast.client.ProxyIf;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Vertex implements ProxyIf, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9019158149126879510L;
	public transient AsynchServiceProxy proxy;
	private String confPath;
	public int groupId;
	public List<Vertex> children;
	public List<Integer> childernIDs;
	// cyclic but for now it easy to have for lca
	public Vertex parent;

	public Vertex(int ID, String configPath, int proxyID) {
		this.confPath = configPath;
		children = new ArrayList<>();
		childernIDs = new ArrayList<>();
		this.groupId = ID;
		// Maybe use async for everithing???
		//change to proxy
		this.proxy = new AsynchServiceProxy(proxyID, confPath);
	}

	// max load
	// max multicast speed

	/**
	 * 
	 * @return a list of vertices of the direct children of the current vertex (one
	 *         depth lower)
	 */
	public List<Vertex> children() {
		return children;
	}

	/**
	 * 
	 * @param groupId
	 *            id of the group you want to know if is reachable from this vertex
	 * @return true if the id is reachable from the current vertex, false otherwise
	 */
	public boolean inReach(int groupId) {
		if (this.groupId == groupId) {
			return true;
		}
		boolean ret = false;
		for (Vertex v : children) {
			ret = v.inReach(groupId);
			if (ret) {
				return true;
			}
		}
		return false;
	}

	@Override
	public AsynchServiceProxy asyncAtomicMulticast(RequestIf req, ReplyListener listener) {
		proxy.invokeAsynchRequest(req.toBytes(), listener, TOMMessageType.ORDERED_REQUEST);
		return null;
	}

	public Vertex findVertexByID(int id) {
		if (id == groupId) {
			return this;
		}
		Vertex ret = null;
		for (Vertex v : children) {
			ret = v.findVertexByID(id);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}
 
	//Service proxy is not serialazable, so ignore with transient and while doing "readObject" create new proxy (I hope it works)
	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization
		oos.defaultWriteObject();

	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();
		this.proxy = new AsynchServiceProxy(groupId, confPath);

	}

}
