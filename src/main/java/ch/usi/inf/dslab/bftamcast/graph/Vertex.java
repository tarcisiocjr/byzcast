/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.graph;

import java.util.ArrayList;
import java.util.List;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import ch.usi.inf.dslab.bftamcast.RequestIf;
import ch.usi.inf.dslab.bftamcast.client.ProxyIf;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Vertex implements ProxyIf {

	public AsynchServiceProxy proxy;
	public int groupId;
	public List<Vertex> connections;
	// cyclic but for now it easy to have for lca
	public Vertex parent;

	public Vertex(int ID, String configPath) {
		connections = new ArrayList<>();
		this.groupId = ID;
		// Maybe use async for everithing???
		this.proxy = new AsynchServiceProxy(groupId, configPath);
	}

	// max load
	// max multicast speed

	/**
	 * 
	 * @return a list of vertices of the direct children of the current vertex (one
	 *         depth lower)
	 */
	public List<Vertex> children() {
		return connections;
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
		for (Vertex v : connections) {
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
		for (Vertex v : connections) {
			ret = v.findVertexByID(id);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}

}
