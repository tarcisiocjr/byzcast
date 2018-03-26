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
	public int capacity, resCapacity;
	List<Vertex> connections = new ArrayList<>();
	Vertex parent;

	public Vertex(int ID, String conf, int capacity) {
		this.ID = ID;
		this.capacity = capacity;
		this.resCapacity = capacity;
	}
}
