package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Vertex {
	public int ID, replicas;
	public double capacity, resCapacity;
	public List<Vertex> connections = new ArrayList<>();
	// private Set<Set<Vertex>> possibleConnections = new HashSet<>();
	public Vertex parent;
	public List<Edge> outgoingEdges = new ArrayList<>();
	public List<Integer> inReach = new ArrayList<>();
	public int inLatency = 0;
	public int level = -1, inDegree = 0;
	public boolean colored = false;

	public Vertex(int ID, String conf, double capacity, int replicas) {
		this.ID = ID;
		this.capacity = capacity;
		this.resCapacity = capacity;
		this.replicas = replicas;

	}

	public String toString() {
		return ID + "";
	}

	public boolean inReach(int groupId) {
		if (this.ID == groupId) {
			return true;
		}
		if (inReach.contains(groupId)) {
			return true;
		}
		boolean ret = false;
		for (Vertex v : connections) {
			ret = v.inReach(groupId);
			if (ret) {
				inReach.add(groupId);
				return true;
			}
		}
		return false;
	}

	public int getLevel() {
		if (level != -1) {
			return level;
		}
		if (parent == null) {
			level = 0;
			return 0;

		} else {
			level = 1 + parent.getLevel();
			return level;
		}
	}

	public int latecyToLCA(Vertex lca) {
		if (this.ID == lca.ID || this.parent == null) {
			return 0;
		} else {
			return inLatency + parent.latecyToLCA(lca);
		}
	}

	public void updateLoad(int load, Set<Vertex> destinations, int replicas, List<Vertex> updated) {
		updated.add(this);
		List<Vertex> toUpdate = new ArrayList<>();
		for (Vertex v : connections) {
			for (Vertex d : destinations) {
				if (!toUpdate.contains(v) && v.inReach(d.ID)) {
					toUpdate.add(v);
				}
			}
		}
		int replies = 0;
		for (Vertex v : toUpdate) {
			replies += v.replicas;
		}

		resCapacity -= load * (replicas + replies);
		for (Vertex v : toUpdate) {
			v.updateLoad(load, destinations, this.replicas, updated);
		}

	}

	/**
	 * @return the iD
	 */
	public int getID() {
		return ID;
	}

	/**
	 * @param iD
	 *            the iD to set
	 */
	public void setID(int iD) {
		ID = iD;
	}

	/**
	 * @return the replicas
	 */
	public int getReplicas() {
		return replicas;
	}

	/**
	 * @param replicas
	 *            the replicas to set
	 */
	public void setReplicas(int replicas) {
		this.replicas = replicas;
	}

	/**
	 * @return the capacity
	 */
	public double getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity
	 *            the capacity to set
	 */
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	/**
	 * @return the resCapacity
	 */
	public double getResCapacity() {
		return resCapacity;
	}

	/**
	 * @param resCapacity
	 *            the resCapacity to set
	 */
	public void setResCapacity(double resCapacity) {
		this.resCapacity = resCapacity;
	}

	public void reset() {
		parent = null;
		connections.clear();
		inLatency = Integer.MAX_VALUE;
		level = -1;
		colored = false;
		inReach.clear();
		resCapacity = capacity;

	}

	 /**
	 * @return the connections
	 */
	 public List<Vertex> getConnections() {
	 return connections;
	 }

	// /**
	// * @param connections the connections to set
	// */
	// public void setConnections(List<Vertex> connections) {
	// this.connections = connections;
	// }

	/**
	 * @return the parent
	 */
	public Vertex getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(Vertex parent) {
		this.parent = parent;
	}

	/**
	 * @return the outgoingEdges
	 */
	public List<Edge> getOutgoingEdges() {
		return outgoingEdges;
	}

	/**
	 * @param outgoingEdges
	 *            the outgoingEdges to set
	 */
	public void setOutgoingEdges(List<Edge> outgoingEdges) {
		this.outgoingEdges = outgoingEdges;
	}

	/**
	 * @return the inReach
	 */
	public List<Integer> getInReach() {
		return inReach;
	}

	/**
	 * @param inReach
	 *            the inReach to set
	 */
	public void setInReach(List<Integer> inReach) {
		this.inReach = inReach;
	}

	/**
	 * @return the inLatency
	 */
	public int getInLatency() {
		return inLatency;
	}

	/**
	 * @param inLatency
	 *            the inLatency to set
	 */
	public void setInLatency(int inLatency) {
		this.inLatency = inLatency;
	}

	/**
	 * @param level
	 *            the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	public void addEdge(Edge edge) {
		this.outgoingEdges.add(edge);

		// not used anymore

		// Set<Set<Vertex>> newPossibleConnections = new HashSet<>();
		// Set<Vertex> extendedExistingconnections;
		// for (Set<Vertex> set : this.possibleConnections) {
		// extendedExistingconnections = new HashSet<>(set);
		// extendedExistingconnections.add(edge.to);
		// newPossibleConnections.add(extendedExistingconnections);
		// }
		// Set<Vertex> newPossiblility = new HashSet<>();
		// newPossiblility.add(edge.to);
		// newPossibleConnections.add(newPossiblility);
		// this.possibleConnections.addAll(newPossibleConnections);
	}

	// public Set<Set<Vertex>> getPossibleConnections(){
	// return this.possibleConnections;
	// }

	// public Set<Set<Vertex>> removeEdge(Edge edge) {
	// this.outgoingEdges.add(edge);
	//
	// Set<Set<Vertex>> toRemove = new HashSet<>();
	// for (Set<Vertex> set : this.possibleConnections) {
	// if(set.contains(edge.to)) {
	// toRemove.add(set);
	// }
	// }
	// this.possibleConnections.removeAll(toRemove);
	// return toRemove;
	//
	// }

	public void resetResCapacity() {
		this.resCapacity = this.capacity;
	}

	public void addConnections(Vertex to) {
		this.connections.add(to);
	}

}
