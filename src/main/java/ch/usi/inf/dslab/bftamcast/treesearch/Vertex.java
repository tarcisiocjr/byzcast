package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Vertex {
	public int ID;
	public double capacity, resCapacity;
	List<Vertex> connections = new ArrayList<>();
	Vertex parent;
	boolean printed = false;
	private List<Integer> inReach = new ArrayList<>();
	public int inDegree = 0;
	public int inLatency = 0;
	int level = -1;
	boolean colored = false;

	public Vertex(int ID, String conf, double capacity) {
		this.ID = ID;
		this.capacity = capacity;
		this.resCapacity = capacity;

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
		if(level!=-1) {
			return level;
		}
		if (parent == null) {
			level = 0;
			return 0;
			
		}
		else {
			level = 1+ parent.getLevel();
			return level;
		}
	}
	
	public int latecyToLCA(Vertex lca) {
		if(this.ID == lca.ID || this.parent == null) {
			return 0;
		}
		else {
			return inLatency + parent.latecyToLCA(lca);
		}
	}

}
