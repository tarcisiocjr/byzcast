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

import bftsmart.tom.AsynchServiceProxy;
import ch.usi.inf.dslab.bftamcast.client.ConsoleClientDirect;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class VertexDirect implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -9019158149126879510L;
	private transient AsynchServiceProxy proxy;
	private String confPath;
	private int groupId;
	private List<VertexDirect> children;
	private List<Integer> childernIDs;
	private List<Integer> inReach;
	// cyclic but for now it easy to have for lca
	private VertexDirect parent;

	public VertexDirect(int ID, String configPath, int proxyID, ConsoleClientDirect c) {
		System.out.println(ID);
		this.confPath = configPath;
		children = new ArrayList<>();
		childernIDs = new ArrayList<>();
		inReach = new ArrayList<>();
		this.groupId = ID;
		this.proxy = new myAsyncProxy(proxyID, confPath, c);
//		ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
//
//		exec.schedule(new Runnable() {
//		          public void run() {
//		        	 
//		          }
//		     }, 0, TimeUnit.SECONDS);
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
		if(inReach.contains(groupId)) {
			return true;
		}
		boolean ret = false;
		for (VertexDirect v : children) {
			ret = v.inReach(groupId);
			if (ret) {
				inReach.add(groupId);
				return true;
			}
		}
		return false;
	}


	/**
	 * check itself and all children (subtree) to find a vertex with a given id
	 * @param id of the vertex you search
	 * @return the vertex if found or null
	 */
	public VertexDirect findVertexByID(int id) {
		if (id == groupId) {
			return this;
		}
		VertexDirect ret = null;
		for (VertexDirect v : children) {
			ret = v.findVertexByID(id);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}
 
	/**
	 * Service proxy is not serialazable, so ignore with transient and while doing "readObject" create new proxy 
	 * @param oos
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization
		oos.defaultWriteObject();

	}

	/**
	 * Service proxy is not serialazable, so ignore with transient and while doing "readObject" create new proxy 
	 * @param ois
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default de-serialization
		ois.defaultReadObject();
		this.proxy = new AsynchServiceProxy(groupId, confPath);

	}

	/**
	 * @return the serial version uid
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
	 * @return the groupId
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * @return the children
	 */
	public List<VertexDirect> getChildren() {
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
	public VertexDirect getParent() {
		return parent;
	}


	/**
	 * @param parent the parent to set
	 */
	public void setParent(VertexDirect parent) {
		this.parent = parent;
	}
	
	public String getConfPath() {
		return confPath;
	}
	

}
