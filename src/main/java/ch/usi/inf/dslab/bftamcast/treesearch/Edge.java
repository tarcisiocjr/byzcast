/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Edge {
	public Vertex from;
	public Vertex to;
	public int latency;
	public boolean used = false;;
	public Edge(Vertex from, Vertex to, int latency) {
		this.from =from;
		this.to = to;
		this.latency = latency;
	}
}
