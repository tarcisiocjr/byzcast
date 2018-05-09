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

import ch.usi.inf.dslab.bftamcast.graph.Vertex;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Tree implements Serializable {
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
		System.out.println(getIdentifier(new int[] { 2, 3 }));
		System.out.println(getIdentifier(new int[] { 0, 2, 3 }));
		System.out.println(getIdentifier(new int[] { 1, 2, 3 }));

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
		Set<Set<Vertex>> allPossibleDestinations = getAllPossibleDestinations(vertices);

		for (Set<Vertex> destination : allPossibleDestinations) {
			int[] destsids = new int[destination.size()];

			int index = 0;
			for (Vertex dest : destination) {
				destsids[index] = dest.ID;
				index++;
			}

			long identifier = getIdentifier(destsids);
			Vertex lcav = lca(destination);
			lcaMap.put(identifier, lcav);
			if (routingmap.get(identifier) != null) {
				System.out.println("collisions!!!!");
				System.out.println("collisions!!!!");

				System.out.println("collisions!!!!");
				System.out.println("collisions!!!!");
				System.out.println("collisions!!!!");
				System.out.println("collisions!!!!");
				System.out.println("collisions!!!!");
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
	 * 
	 * @param vetices
	 *            list of vertices you are interest into knowing their lowest common
	 *            ancestor
	 * @return the lowest common ancestor in the tree of all the vertices in the
	 *         input vertices list.
	 */
	private Vertex lca(Set<Vertex> vertices) {

		// tree only has one path between any two nodes, so only one child of root could
		// be ancestor
		Vertex ancestor = root;
		boolean reachable = true;
		while (reachable) {
			reachable = true;
			// if you can not go lower in the tree return current acestor
			if (ancestor.connections.isEmpty()) {
				return ancestor;
			}
			// check if any of the current ancestor's childrens can reach all destinations
			for (Vertex v : ancestor.connections) {
				reachable = true;
				for (Vertex target : vertices) {
					// check child reach for all destinations
					reachable = reachable & v.inReach(target.ID);
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

	// // generate all possible desitations
	public static Set<Set<Vertex>> getAllPossibleDestinations(List<Vertex> vertices) {
		Set<Set<Vertex>> destinations = new HashSet<>();
		getAllPossibleDestinations2(vertices, 0, destinations, new HashSet<>());
		return destinations;
	}

	private static void getAllPossibleDestinations2(List<Vertex> vertices, int indexInList,
			Set<Set<Vertex>> destinations, Set<Vertex> previousSet) {
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

	public Set<Vertex> getRoute(long identifier, Vertex me) {

		return routingmap.get(identifier).get(me);

	}

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

	public Vertex getLca(long destIdentifier) {
		return lcaMap.get(destIdentifier);
	}

}
