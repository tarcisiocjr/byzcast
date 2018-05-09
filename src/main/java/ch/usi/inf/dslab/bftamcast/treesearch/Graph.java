/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import ch.usi.inf.dslab.bftamcast.graph.Vertex;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<Load> loads = new ArrayList<>();
	public int numerOfReplicas = 4;
	public static long bestbestscore = Long.MAX_VALUE;

	// instead have a sorter for each destination combo
	public static void main(String[] args) throws Exception {
		new Graph("config/load.conf");
	}

	public Graph(String configFile) throws Exception {

		// parse edges, vertices, load and constrains
		FileReader fr;
		BufferedReader rd = null;

		try {
			fr = new FileReader(configFile);

			rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				// comment line
				if (!line.startsWith("#") && !line.isEmpty()) {
					StringTokenizer str;
					// load line
					if (line.contains("m/s")) {
						str = new StringTokenizer(line, "m/s");
						if (str.countTokens() == 2) {
							int msgSec = Integer.valueOf(str.nextToken());
							Set<Vertex> destinations = new HashSet<>();
							str = new StringTokenizer(str.nextToken(), " ");
							while (str.hasMoreTokens()) {
								int id = Integer.valueOf(str.nextToken());
								for (Vertex vertex : vertices) {
									if (vertex.getID() == id) {
										destinations.add(vertex);
										break;
									}
								}
							}
							// create destination load
							Load load = new Load(msgSec, destinations);
							loads.add(load);
						}
					} else {
						// vertex declaration (group)
						str = new StringTokenizer(line, " ");
						if (str.countTokens() == 3) {
							Vertex newVertex = new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(),
									Integer.valueOf(str.nextToken()), numerOfReplicas);
							for (Vertex vertex : vertices) {
								if (vertex.getID() == newVertex.getID()) {
									System.err.println("duplicate vertex id");
								}
							}
							vertices.add(newVertex);
						}
						// edge declaration latency
						else if (str.countTokens() == 4) {
							int vertexAid = Integer.valueOf(str.nextToken());
							str.nextToken(); // drop "-"
							int vertexBid = Integer.valueOf(str.nextToken());
							int latency = Integer.valueOf(str.nextToken());
							Vertex vertexA = null, vertexB = null;
							for (Vertex v : vertices) {
								if (vertexAid == v.getID()) {
									vertexA = v;
								}
								if (vertexBid == v.getID()) {
									vertexB = v;
								}
							}
							if (vertexA == null || vertexB == null || vertexA == vertexB) {
								System.err.println("connection not know for edge or self edge");
								return;
							}

							for (Edge edge : edges) {
								if ((edge.from == vertexA && edge.to == vertexB)
										|| (edge.to == vertexA && edge.from == vertexB)) {
									System.err.println("duplicate edge");
									return;
								}
							}
							// edges are bidirectional
							Edge edge1 = new Edge(vertexA, vertexB, latency);
							Edge edge2 = new Edge(vertexB, vertexA, latency);
							vertexA.addEdge(edge1);
							vertexB.addEdge(edge2);
							edges.add(edge1);
							edges.add(edge2);
						}

					}
				}

			}
			fr.close();
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (rd != null) {
				try {
					rd.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		// testing generate tree

//		 int originalSize = vertices.size();
		 int originalSize = 0;
//		 for (int i = vertices.size()-1; i < 1; i++) {
		 for (int i = 0; i < 9; i++) {
		 vertices.add(new Vertex(i, "", 100000, numerOfReplicas));
		 }
		for (Vertex vertex1 : vertices) {
			for (Vertex vertex2 : vertices) {
				vertex1.setCapacity(Integer.MAX_VALUE);
				vertex1.setResCapacity(Integer.MAX_VALUE);
				vertex2.setCapacity(Integer.MAX_VALUE);
				vertex2.setResCapacity(Integer.MAX_VALUE);
				if (vertex1 != vertex2) {
					Edge edge = new Edge(vertex1, vertex2, 100);
					vertex1.addEdge(edge);
					edges.add(edge);
				}
			}
		}
		System.out.println("done generating test");

		// generate all possible destinations, not all might be specified, and assign
		// base load (1m/s)ß
		int baseload = 1;
		// generate all dests and add not specified ones
		List<Set<Vertex>> allPossibleDests = getAllPossibleDestinations(vertices);
		System.out.println("done generating dests");
		// Random r = new Random();
		// for(List<Vertex> f : allDests) {
		// for (Vertex v : f) {
		// System.out.print(v.ID+ " ,");
		// }
		// System.out.println();
		// }
		for (Load load : loads) {
			Set<Vertex> toremove = null;
			for (Set<Vertex> destination : allPossibleDests) {
				if (destination.containsAll(load.destinations)) {
					toremove = destination;
					// System.out.println("fasljdfdksajfkljadslkfjladskfjlask");
					break;
				}
			}
			allPossibleDests.remove(toremove);
		}

		for (Set<Vertex> destination : allPossibleDests) {
			loads.add(new Load(baseload, destination));
		}
		System.out.println("sets dest size = " + loads.size());
		System.out.println("vert size = " + vertices.size());
		System.out.println("edges size = " + edges.size());

		loads.sort(new Load(0, null));

		System.out.println("max load size = " + loads.get(0).load);

		List<List<Edge>> trees = new ArrayList<>();

		generateTrees(vertices, trees, loads);

		if (!trees.isEmpty()) {
			printTree(trees.get(trees.size() - 1), -100);
		}
		
		Runtime.getRuntime().exec("say finished");
		Runtime.getRuntime().exec("say finished");
		Runtime.getRuntime().exec("say finished");
		Runtime.getRuntime().exec("say finished");
		Runtime.getRuntime().exec("say finished");

	}

	public void generateTrees(List<Vertex> vertices, List<List<Edge>> trees, List<Load> loads) {

		long start = 0, end = 0;

		System.out.println("start generating trees");
		start = System.nanoTime();

		// TODO recycle all destination when generating all loads initially
		List<Set<Vertex>> possibleChilds = getAllPossibleDestinations(vertices);
		Comparator<Set<Vertex>> comp = new Comparator<Set<Vertex>>() {
			public int compare(Set<Vertex> a1, Set<Vertex> a2) {
				return a2.size() - a1.size(); // assumes you want biggest to smallest
			}
		};
		Collections.sort(possibleChilds, comp);
		time = System.nanoTime();
		double numberOfVertices = vertices.size();
		numberOfTrees = Math.pow(numberOfVertices, numberOfVertices - 1);
		for (Vertex root : vertices) {
			for (Vertex vertex : vertices) {
				vertex.reset();
			}
			iteration = 0;
			List<Vertex> visited = new ArrayList<>();
			Set<Set<Vertex>> cleanSet = new HashSet<>();
			for (Set<Vertex> set : possibleChilds) {
				if (!set.contains(root)) {
					cleanSet.add(set);
				}
			}
			visited.add(root);
			List<Vertex> fringe = new ArrayList<>();
			fringe.add(root);

			generateTreesRec(root, new ArrayList<>(), visited, trees, vertices.size(), cleanSet, fringe, loads, 0,
					new ArrayList<>());

		}
		end = System.nanoTime();

		System.out.println("#generated trees are:  " + trees.size() + "  expected: " + ((long) numberOfTrees)
				+ "   total iterations: " + iteration);

		System.out.println("one set took : " + (end - start) + "  nanosecons");

	}

	public static int iteration = 0;
	public static long time = 0;
	public static double numberOfTrees = 0;
	public static DecimalFormat myFormat = new DecimalFormat("0.00000000");

	// GOOD algorithm! works no dups, tested up to 8 vertices, generates all trees
	// n^(n-1)
	public void generateTreesRec(Vertex root, List<Edge> tree, List<Vertex> visited, List<List<Edge>> trees,
			int numVertices, Set<Set<Vertex>> possibleChilds, List<Vertex> fringe, List<Load> loads, long prevscore,
			List<Edge> prevtree) {

		iteration++;
		long score = compute_score(root, tree, loads, bestbestscore, vertices, prevscore, prevtree);
		//

//		if (score >= bestbestscore) {
//			return;
//		}

		if (tree.size() == numVertices - 1) {
			if (score >= bestbestscore) {
				return;
			}
			System.out.println("new best " + score);
			// printTree(tree, iteration) ;
			trees.add(new ArrayList<>(tree));
			bestbestscore = score;
			// if (System.nanoTime() - time >= 1 * 1e9) {
			// System.out.println(myFormat.format((((((double) trees.size()) /
			// numberOfTrees) * 100))) + "%");
			// time = System.nanoTime();
			// }
			return;
		}

		List<Vertex> keptFringe = new ArrayList<>(fringe);

		for (Vertex visiting : fringe) {
			keptFringe.remove(visiting);
			for (Set<Vertex> childs : possibleChilds) {

				// new tree
				List<Edge> newTree = new ArrayList<>();

				// prune possibilities of growing tree
				Set<Set<Vertex>> newPossibleChilds = new HashSet<>(possibleChilds);
				Set<Set<Vertex>> toremove = new HashSet<>();

				int count = 0;
				for (Edge e : visiting.getOutgoingEdges()) {
					if (childs.contains(e.to)) {
						count++;
						newTree.add(e);
						// prune set
						for (Set<Vertex> set : newPossibleChilds) {
							if (set.contains(e.to)) {
								toremove.add(set);
							}
						}
						newPossibleChilds.removeAll(toremove);
						toremove.clear();
					}
				}
				// case of not connected graph a vertex might not be connected to all other
				// vertices
				if (count == childs.size()) {
					newTree.addAll(tree);

					List<Vertex> newVisited = new ArrayList<>(visited);
					newVisited.addAll(childs);

					List<Vertex> newFringe = new ArrayList<>(keptFringe);
					newFringe.remove(visiting);
					newFringe.addAll(childs);
					List<Vertex> copy = copyVertices(vertices);

					generateTreesRec(root, newTree, newVisited, trees, numVertices, newPossibleChilds, newFringe, loads,
							score, tree);

					resetVertices(copy, vertices);

				}

			}
		}
	}

	public List<Vertex> copyVertices(List<Vertex> vertices) {
		List<Vertex> copy = new ArrayList<>();
		for (Vertex vertex : vertices) {
			copy.add(new Vertex(vertex));

		}

		return copy;
	}

	public void resetVertices(List<Vertex> source, List<Vertex> destination) {
		for (int i = 0; i < source.size(); i++) {
			destination.get(i).copysett(source.get(i));
		}

	}

	public static long compute_score(Vertex root, List<Edge> tree, List<Load> loads, long minscore,
			List<Vertex> vertices, long prevscore, List<Edge> prevtree) {
		long score = prevscore;
		// System.out.println(root.resCapacity);

		// for (Vertex vertex : vertices) {
		// vertex.reset();
		// }

		List<Vertex> treevertices = new ArrayList<>();
		treevertices.add(root);
		List<Vertex> prevtreevertices = new ArrayList<>();
		prevtreevertices.add(root);

		for (Edge edge : tree) {
			if (prevtreevertices.contains(edge.to)) {
				prevtreevertices.add(edge.to);
			}
			treevertices.add(edge.to);
			edge.from.addConnections(edge.to);
			edge.to.setParent(edge.from);
			edge.to.setInLatency(edge.latency);
		}

		// System.out.println(oldtreevertices.size());
		for (Load load : loads) {
			// compute only if current tree contains all groups and not computed for
			// previous tree
			if (treevertices.containsAll(load.destinations) && !prevtreevertices.containsAll(load.destinations)) {
				// find lca and lca heigh in tree
				Vertex lca = lca(load.destinations, root);
				int lcaH = lca.getLevel();

				List<Vertex> updated = new ArrayList<>();
				lca.updateLoad(load.load, load.destinations, 1, updated);

				boolean saturated = false;
				for (Vertex vertex : updated) {
					// check if capacity is not saturated
					if (vertex.getResCapacity() <= 0) {
						// System.out.println("saturated!!!!");
						return Long.MAX_VALUE;
					}
				}

				// can remove since returns when saturated
				if (!saturated) {
					for (Vertex v : load.destinations) {

						// compute score for load on destination set
						long val = (v.latecyToLCA(lca)
								+ (v.getLevel() - lcaH) * (load.load / load.destinations.size()));
						// System.out.println(val);
						score += val;
						// if already worst stop computing
						if (score >= minscore) {
							return Long.MAX_VALUE;
						}
					}
					// if already worst stop computing
					if (score >= minscore) {
						return Long.MAX_VALUE;
					}
				}
			}

		}

		return score;
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

	public static void printTree(List<Edge> tree, int id) {
		if (tree != null) {
			PrintWriter writer;
			try {

				String ggq = "digraph G { ";
				for (Edge v : tree) {

					if (!ggq.contains("" + v.from.getID() + "->" + v.to.getID() + "\n")) {
						ggq += "" + v.from.getID() + "->" + v.to.getID() + "\n";
					}
				}

				ggq += "}";
				writer = new PrintWriter("graphs/graph_totassl" + id + ".dot", "UTF-8");
				writer.println(ggq);
				writer.close();

			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		}
	}

	public boolean existsLoad(Set<Vertex> dests, List<Load> loads) {
		for (Load load : loads) {
			if (load.matchDests(dests)) {
				return true;
			}
		}
		return false;
	}
}
