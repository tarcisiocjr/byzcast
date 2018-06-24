package ch.usi.inf.dslab.bftamcast;

import java.util.List;
import java.util.Set;

import ch.usi.inf.dslab.bftamcast.graph.Vertex;

public interface OverlayTree {
	public long getIdentifier(int[] destinations);
	public Set<Vertex> getRoute(long identifier, Vertex me);
	public Vertex getRoot();
	public Vertex findVertexById(int id);
	public Vertex getLca(long destIdentifier);
	public List<Integer> getDestinations();

}
