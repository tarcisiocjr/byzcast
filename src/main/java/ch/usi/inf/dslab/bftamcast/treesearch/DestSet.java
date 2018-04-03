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
	public int percentage;

	public Vertex root;
	public List<DestSet> overlaps = new ArrayList<>();


	public DestSet(int percentage, List<Vertex> dests) {
		this.percentage = percentage;
		this.destinations =  dests;
		if(destinations !=null) {
		for (Vertex vertex : dests) {
			destinationsIDS.add(vertex.ID);
		}}
		
	}

	public boolean matchDests(List<Integer> dests) {
		for (Integer i : destinationsIDS) {
			if (!dests.contains(i)) {
				return false;
			}
		}
		for (Integer i : dests) {
			if (!destinationsIDS.contains(i)) {
				return false;
			}
		}
		return true;
	} 


	@Override
	public int compare(DestSet arg0, DestSet arg1) {
		
		return   arg1.percentage -arg0.percentage;
	}
}
