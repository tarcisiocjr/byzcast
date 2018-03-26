/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class DestSet {
	public List<Vertex> destinations = new ArrayList<>();
	public int percentage;

	public DestSet(int percentage) {
		this.percentage = percentage;
	}
}
