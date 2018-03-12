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

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Vertex implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9019158149126879510L;
	private transient AsynchServiceProxy proxy;
	private String confPath;
	private int groupId;
	private List<Vertex> children;
	private List<Integer> childernIDs;
	// cyclic but for now it easy to have for lca
	public Vertex parent;

	public Vertex(int ID, String configPath, int proxyID) {
		this.confPath = configPath;
		children = new ArrayList<>();
		childernIDs = new ArrayList<>();
		this.groupId = ID;
		this.proxy = new AsynchServiceProxy(proxyID, confPath);
	}

	// max load
	// max multicast speed



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

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the proxy
	 */
	public AsynchServiceProxy getProxy() {
		return proxy;
	}

	/**
	 * @return the confPath
	 */
	public String getConfPath() {
		return confPath;
	}

	/**
	 * @return the groupId
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * @return the children
	 */
	public List<Vertex> getChildren() {
		return children;
	}

	/**
	 * @return the childernIDs
	 */
	public List<Integer> getChildernIDs() {
		return childernIDs;
	}

	/**
	 * @return the parent
	 */
	public Vertex getParent() {
		return parent;
	}

	/**
	 * @param proxy the proxy to set
	 */
	public void setProxy(AsynchServiceProxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * @param confPath the confPath to set
	 */
	public void setConfPath(String confPath) {
		this.confPath = confPath;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<Vertex> children) {
		this.children = children;
	}

	/**
	 * @param childernIDs the childernIDs to set
	 */
	public void setChildernIDs(List<Integer> childernIDs) {
		this.childernIDs = childernIDs;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Vertex parent) {
		this.parent = parent;
	}
	
	
	

}
