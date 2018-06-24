package ch.usi.inf.dslab.bftamcast.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.usi.inf.dslab.bftamcast.graph.Vertex;

public class VertexUtilities {

	/**
	 * 
	 * @param vetices
	 *            list of vertices you are interest into knowing their lowest common
	 *            ancestor and the root of the tree
	 * @return the lowest common ancestor in the tree of all the vertices in the
	 *         input vertices list.
	 */
	public static Vertex lca(Set<Vertex> vertices, Vertex root) {

		// tree only has one path between any two nodes, so only one child of root could
		// be ancestor
		Vertex ancestor = root;
		boolean reachable = true;
		while (reachable) {
			reachable = true;
			// if you can not go lower in the tree return current acestor
			if (ancestor.getConnections().isEmpty()) {
				return ancestor;
			}
			// check if any of the current ancestor's childrens can reach all destinations
			for (Vertex v : ancestor.getConnections()) {
				reachable = true;
				for (Vertex target : vertices) {
					// check child reach for all destinations
					reachable = reachable & v.inReach(target.getID());
					if (!reachable) {
						break;
					}
				}
				// if child can reach all it is the new ancestor
				if (reachable) {
					// tree only one path between two vertices, so if found lower anchestor it is
					// not needed to keep searching other children
					ancestor = v;
					break;
				}
			}
		}
		return ancestor;
	}
	
	
	
	// // generate all possible desitations
	public static List<Set<Vertex>> getAllPossibleDestinations(List<Vertex> vertices) {
		List<Set<Vertex>> destinations = new ArrayList<>();
		getAllPossibleDestinations2(vertices, 0, destinations, new HashSet<>());
		return destinations;
	}

	private static void getAllPossibleDestinations2(List<Vertex> vertices, int indexInList,
			List<Set<Vertex>> destinations, Set<Vertex> previousSet) {
		if (indexInList >= vertices.size()) {
			return;
		}
		previousSet.add(vertices.get(indexInList));
		destinations.add(new HashSet<>(previousSet));
		// consider vertex
		getAllPossibleDestinations2(vertices, indexInList + 1, destinations, previousSet);
		// skip vertex
		previousSet.remove(vertices.get(indexInList));
		getAllPossibleDestinations2(vertices, indexInList + 1, destinations, previousSet);
	}
	
	/**
	 * from set of destinations generate a unique identifier
	 */
	public static long getIdentifier(int[] destinations) {

		Arrays.sort(destinations);
		String d = "";
		for (int i = 0; i < destinations.length; i++) {
//			System.out.print(destinations[i] + "  ");
			d += destinations[i] + "-";
		}
//		System.out.println();

		long hash = 7;
		for (int i = 0; i < d.length(); i++) {
			hash = hash * 31 + d.charAt(i);
		}
		return hash;
	}


}
