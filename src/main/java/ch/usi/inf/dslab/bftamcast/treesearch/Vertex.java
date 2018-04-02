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

	public Vertex(int ID, String conf, double capacity) {
		this.ID = ID;
		this.capacity = capacity;
		this.resCapacity = capacity;
	}
}
