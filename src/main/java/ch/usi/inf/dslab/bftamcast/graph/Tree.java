/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import ch.usi.inf.dslab.bftamcast.OverlayTree;
import ch.usi.inf.dslab.bftamcast.graph.Vertex;
import ch.usi.inf.dslab.bftamcast.util.VertexUtilities;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Tree implements Serializable, OverlayTree {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1340477045756411763L;
	private Vertex root;
	private List<Integer> destinations;
	private final int replicas = 4;

	private Map<Long, Map<Vertex, Set<Vertex>>> routingmap = new HashMap<>();
	private Map<Long, Vertex> lcaMap = new HashMap<>();

	/**
	 * Main for testing
	 * 
	 * @param args
	 *            none
	 */
	public static void main(String[] args) {

	}

	/**
	 * getter for list of destinations in the tree
	 * 
	 * @return the field destinations containing the id of all destinations in the
	 *         tree
	 */
	public List<Integer> getDestinations() {
		return destinations;
	}

	/**
	 * Constructor
	 * 
	 * @param configFile
	 *            containing the id of the vertices and their config path for bft
	 *            smart and connection between them
	 */
	public Tree(String configFile, int proxyID) {

		destinations = new ArrayList<>();
		List<Vertex> vertices = new ArrayList<>();
		FileReader fr;
		BufferedReader rd;
		try {
			fr = new FileReader(configFile);
			rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				if (!line.startsWith("#") && !line.isEmpty()) {
					// TODO use Graph tree builder
					StringTokenizer str = new StringTokenizer(line, " ");
					// vertex declaration (group)
					if (str.countTokens() == 2) {
						vertices.add(new Vertex(Integer.valueOf(str.nextToken()),
								configFile.replace("tree.conf", "") + str.nextToken(), 0, replicas, proxyID));
						destinations.add(vertices.get(vertices.size() - 1).getID());
						// System.out.println("adding vertex: " + vertices.get(vertices.size()-1));
					}
					// connection declaration
					if (str.countTokens() == 3) {

						int from = Integer.valueOf(str.nextToken());
						str.nextToken();// throw away "->"
						int to = Integer.valueOf(str.nextToken());

						// System.out.println("adding edge: " + from +" -> " + to);

						// add connections in vertices
						for (Vertex v1 : vertices) {
							if (v1.getID() == from) {
								for (Vertex v2 : vertices) {
									if (v2.getID() == to) {
										v1.getConnections().add(v2);
										v2.setParent(v1);
									}
								}
							}
						}
					}
				}
			}
			// the vertex with no parent is the root vertex of the tree
			for (Vertex v : vertices) {
				if (v.getParent() == null) {
					root = v;
				}
			}

			fr.close();
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Targets>>
		List<Set<Vertex>> allPossibleDestinations = VertexUtilities.getAllPossibleDestinations(vertices);

		for (Set<Vertex> destination : allPossibleDestinations) {
			int[] destsids = new int[destination.size()];

			int index = 0;
			for (Vertex dest : destination) {
				destsids[index] = dest.ID;
				index++;
			}

			long identifier = getIdentifier(destsids);
			Vertex lcav = VertexUtilities.lca(destination, root);
			lcaMap.put(identifier, lcav);
			if (routingmap.get(identifier) != null) {
				System.err.println("collisions!!!!");
			}

			routingmap.put(identifier, new HashMap<>());
			for (Vertex vertex : vertices) {
				Set<Vertex> toSendto = new HashSet<>();
				for (Vertex dest : destination) {
					// I am a target, compute but wait for majority of other destination to
					// execute
					// the same to asnwer
					if (vertex == dest) {
						toSendto.add(vertex);
					}
					// my child in tree is a destination, forward it
					else if (vertex.getConnections().contains(dest)) {
						toSendto.add(dest);
					}
					// destination must be in the path of only one of my childrens
					else {

						for (Vertex child : vertex.getConnections()) {
							if (child.inReach(dest.ID)) {
								toSendto.add(child);
								break;// only one path
							}
						}
					}
				}
				routingmap.get(identifier).put(vertex, toSendto);
			}
		}

	}

	

	/**
	 * find vertex in tree from it's id
	 * 
	 * @param id
	 * @return vertex with specified id or null
	 */
	public Vertex findVertexById(int id) {
		// System.out.println(root);
		return root.findVertexByID(id);
	}

	public Vertex getRoot() {
		return root;
	}



	public Set<Vertex> getRoute(long identifier, Vertex me) {

		return routingmap.get(identifier).get(me);

	}

	
	
	/**
	 * from set of destinations generate a unique identifier
	 */
	public long getIdentifier(int[] destinations) {

		return VertexUtilities.getIdentifier(destinations);
	}

	public Vertex getLca(long destIdentifier) {
		return lcaMap.get(destIdentifier);
	}

}