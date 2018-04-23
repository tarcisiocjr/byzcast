/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Load implements Comparator<Load>{
	public Set<Vertex> destinations = new HashSet<>();
	public Set<Integer> destinationsIDS = new HashSet<>();
	public int load;
	public int penalty = 0;

	public Vertex root;
	public List<Load> overlaps = new ArrayList<>();


	public Load(int load, Set<Vertex> dests) {
		this.load = load;
		this.destinations =  dests;
		if(destinations !=null) {
		for (Vertex vertex : dests) {
			destinationsIDS.add(vertex.getID());
		}}
		
	}

	public boolean matchDests(Set<Vertex> dests) {
		for (Vertex i : dests) {
			if (!destinationsIDS.contains(i.getID())) {
				return false;
			}
		}
		for (Vertex i : destinations) {
			if (!dests.contains(i)) {
				return false;
			}
		}
		return true;
	} 


	@Override
	public int compare(Load arg0, Load arg1) {
		
		return   arg1.load -arg0.load;
	}
}
