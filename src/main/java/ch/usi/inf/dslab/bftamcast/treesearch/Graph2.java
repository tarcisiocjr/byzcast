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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph2 {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<Load> loads = new ArrayList<>();
	public int numerOfReplicas = 4;
	public static long bestbestscore =Long.MAX_VALUE;

	// instead have a sorter for each destination combo
	public static void main(String[] args) throws Exception {
		new Graph2("config/load.conf");
	}

	public Graph2(String configFile) throws Exception {

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

		// int originalSize = vertices.size();
		// for (int i = vertices.size()-1; i < 4; i++) {
		// vertices.add(new Vertex(i, "", 100000, numerOfReplicas));
		// }
		for (Vertex vertex1 : vertices) {
			for (Vertex vertex2 : vertices) {
//				vertex1.setCapacity(100000);
//				vertex1.setResCapacity(100000);
//				vertex2.setCapacity(100000);
//				vertex2.setResCapacity(100000);
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
		Set<Set<Vertex>> allPossibleDests = getAllPossibleDestinations(vertices);
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
			 if (!existsLoad(destination, loads)) {

			// load.add(new DestSet(r.nextInt(5)+1, d));
			loads.add(new Load(baseload, destination));
			 }
		}
		System.out.println("sets dest size = " + loads.size());
		System.out.println("vert size = " + vertices.size());
		System.out.println("edges size = " + edges.size());



		loads.sort(new Load(0, null));

		List<List<Edge>> trees = new ArrayList<>();

		generateTrees(vertices, trees, loads);
		
		

		System.out.println("#generated trees are:  " + trees.size() + "  expected: " + ((long) numberOfTrees)
				+ "   total iterations: " + iteration);
		
		if(!trees.isEmpty()) {
		printTree(trees.get(trees.size()-1), -100);
		}
		

	}

	public void generateTrees(List<Vertex> vertices, List<List<Edge>> trees, List<Load> loads) {
		// TODO recycle all destination when generating all loads initially
		Set<Set<Vertex>> possibleChilds = getAllPossibleDestinations(vertices);
		time = System.nanoTime();
		double numberOfVertices = vertices.size();
		numberOfTrees = Math.pow(numberOfVertices, numberOfVertices - 1);
		long start = 0, end = 0;
		for (Vertex root : vertices) {
			for(Vertex vertex : vertices) {
				vertex.setParent(null);
				vertex.getConnections().clear();
				vertex.setInLatency(Integer.MAX_VALUE);
				vertex.setLevel(-1);
				vertex.getInReach().clear();
				vertex.resetResCapacity();
			}
			iteration=0;
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
			start = System.nanoTime();

			generateTreesRec(root, new ArrayList<>(), visited, trees, vertices.size(), cleanSet, fringe, loads, 0 , new ArrayList<>());

			end = System.nanoTime();

		}
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
	public void generateTreesRec(Vertex  root, List<Edge> tree, List<Vertex> visited, List<List<Edge>> trees, int numVertices,
			Set<Set<Vertex>> possibleChilds, List<Vertex> fringe, List<Load> loads, long previousScore, List<Edge> prevTree)
	{

		iteration++;
//		 long score = compute_score(root, tree, loads, bestbestscore, vertices,previousScore, prevTree);
		long score = compute_score(root, tree, loads, bestbestscore, vertices,0, new ArrayList<>());
//		 if (score >= bestbestscore ) {
//		 // System.out.println("prune " + bestbestscore + " " + bestScore + " " +
////		 score);
//		 return;
//		 }

		if (tree.size() == numVertices - 1) {
			  System.out.println("new best " + score );
			// printTree(tree, iteration) ;
			trees.add(new ArrayList<>(tree));
			bestbestscore = score;
//			if (System.nanoTime() - time >= 1 * 1e9) {
//				System.out.println(myFormat.format((((((double) trees.size()) / numberOfTrees) * 100))) + "%");
//				time = System.nanoTime();
//			}
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
					generateTreesRec(root, newTree, newVisited, trees, numVertices, newPossibleChilds, newFringe, loads, score, tree);
				}

			}
		}
	}

	public static long compute_score(Vertex root, List<Edge> tree, List<Load> loads, long minscore, List<Vertex> vertices,
			long prevScore, List<Edge> prevTree) {

		long score = prevScore;
		
		
		
//		for (Edge edge : tree) {
//			Vertex v = edge.from;
//			v.inDegree = 0;
//			v.parent = null;
//			v.connections.clear();
//			v.inLatency = Integer.MAX_VALUE;
//			v.level = -1;
//			v.colored = false;
//			v.inReach.clear();
//			v.resCapacity = v.capacity;
//			v = edge.to;
//			v.inDegree = 0;
//			v.parent = null;
//			v.connections.clear();
//			v.inLatency = Integer.MAX_VALUE;
//			v.level = -1;
//			v.colored = false;
//			v.inReach.clear();
//			v.resCapacity = v.capacity;
//		}
		

		List<Vertex> treevertices = new ArrayList<>();
		List<Vertex> oldtreevertices = new ArrayList<>();
		treevertices.add(root);
		oldtreevertices.add(root);
		for (Edge edge : tree) {
//			if (prevTree.contains(edge)) {
//				oldtreevertices.add(edge.to);
//			} else {
				edge.to.getConnections().clear();
				edge.to.getInReach().clear();
				edge.to.setLevel(-1);
				edge.to.resetResCapacity();
				edge.to.setParent(edge.from);
				edge.to.setInLatency(edge.latency);
				
				oldtreevertices.remove(edge.from);
				edge.from.getConnections().clear();
				edge.from.getInReach().clear();
				edge.from.resetResCapacity();
				
				edge.from.getConnections().add(edge.to);
				edge.from.getInReach().add(edge.to.getID());
				

//			}
			treevertices.add(edge.to);
		}

		for (Load load : loads) {
			// compute only if current tree contains all groups and not computed for
			// previous tree
			if (treevertices.containsAll(load.destinations) ) {// && !oldtreevertices.containsAll(load.destinations)) {
				// find lca and lca heigh in tree
				Vertex lca = lca(load.destinations, root);
				int lcaH = lca.getLevel();

				List<Vertex> updated = new ArrayList<>();
				lca.updateLoad(load.load, load.destinations, 1, updated);

				boolean saturated = false;
				for (Vertex vertex : updated) {
					// check if capacity is not saturated
					if (vertex.getResCapacity() <= 0) {
//						System.out.println("saturated!!!!");
						return Long.MAX_VALUE;
					}
				}

				// can remove since returns when saturated
				if (!saturated) {
					for (Vertex v : load.destinations) {

						// compute score for load on destination set
						long val = (v.latecyToLCA(lca) + (v.getLevel() - lcaH) * (load.load / load.destinations.size()));
//						System.out.println(val);
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

		if (score == 15904) {
			System.out.println(treevertices.size());
		}
		return score;
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
