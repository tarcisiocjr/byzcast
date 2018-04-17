/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class DestSet implements Comparator<DestSet>{
	public List<Vertex> destinations = new ArrayList<>();
	public List<Integer> destinationsIDS = new ArrayList<>();
	public int load;
	public int penalty = 0;

	public Vertex root;
	public List<DestSet> overlaps = new ArrayList<>();


	public DestSet(int load, List<Vertex> dests) {
		this.load = load;
		this.destinations =  dests;
		if(destinations !=null) {
		for (Vertex vertex : dests) {
			destinationsIDS.add(vertex.ID);
		}}
		
	}

	public boolean matchDests(List<Vertex> dests) {
		for (Vertex i : dests) {
			if (!destinationsIDS.contains(i.ID)) {
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
	public int compare(DestSet arg0, DestSet arg1) {
		
		return   arg1.load -arg0.load;
	}
}
