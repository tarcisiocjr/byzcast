package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.List;

import bftsmart.tom.TOMSender;

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
		if (parent == null)
			return 0;
		else
			return 1+ parent.getLevel();
	}

}
