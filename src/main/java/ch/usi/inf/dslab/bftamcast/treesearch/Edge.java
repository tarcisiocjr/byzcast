/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Edge {
	public Vertex a;
	public Vertex b;
	public int latency;
	public Edge(Vertex a, Vertex b, int latency) {
		this.a =a;
		this.b = b;
		this.latency = latency;
	}
}
