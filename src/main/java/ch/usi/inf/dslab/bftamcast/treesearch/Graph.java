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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<DestSet> load = new ArrayList<>();
	public int numerOfReplicas = 4;
	public static int bestbestscore = Integer.MAX_VALUE;

	// instead have a sorter for each destination combo
	public static void main(String[] args) {
		new Graph("config/load.conf");
	}

	public Graph(String configFile) {

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
							int loadp = Integer.valueOf(str.nextToken());
							List<Vertex> ver = new ArrayList<>();
							str = new StringTokenizer(str.nextToken(), " ");
							while (str.hasMoreTokens()) {
								int id = Integer.valueOf(str.nextToken());
								for (Vertex v : vertices) {
									if (v.ID == id) {
										ver.add(v);
										break;
									}
								}
							}
							// create destination load
							DestSet s = new DestSet(loadp, ver);
							load.add(s);
						}
					} else {
						// vertex declaration (group)
						str = new StringTokenizer(line, " ");
						if (str.countTokens() == 3) {
							Vertex v = new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(),
									Integer.valueOf(str.nextToken()));
							v.replicas = numerOfReplicas;
							vertices.add(v);
						}
						// edge declaration latency
						else if (str.countTokens() == 4) {
							int a = Integer.valueOf(str.nextToken());
							str.nextToken(); // drop "-"
							int b = Integer.valueOf(str.nextToken());
							int latency = Integer.valueOf(str.nextToken());
							Vertex aa = null, bb = null;
							for (Vertex v : vertices) {
								if (a == v.ID) {
									aa = v;
								}
								if (b == v.ID) {
									bb = v;
								}
							}
							if (aa == null || bb == null) {
								System.err.println("connection not know for edge");
								return;
							}
							// edges are bidirectional
							Edge e1 = new Edge(aa, bb, latency);
							Edge e2 = new Edge(bb, aa, latency);
							aa.outgoingEdges.add(e1);
							bb.outgoingEdges.add(e2);

							edges.add(e1);
							edges.add(e2);
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

		// generate all possible destinations, not all might be specified, and assign
		// base load (1m/s)
		int baseload = 1;
		// generate all dests and add not specified ones
		List<List<Vertex>> allDests = getAlldestinations(vertices);
		for (List<Vertex> d : allDests) {
			if (!existsLoad(d)) {
				load.add(new DestSet(baseload, d));
			}
		}

		// find best possible tree
		int minscore = Integer.MAX_VALUE;
		List<Edge> topTree = null;

		// build base tree;

		// use

		// generate all possible combination of edges (change to generate only the ones
		// of size v-1)
		load.sort(new DestSet(0, null));
		List<List<Edge>> gg = getSubsets(edges, vertices.size() - 1);
		List<List<Edge>> ggremove = new ArrayList<>();
		System.out.println("All kset to explore       " + gg.size());
		int count = 0;
		for (List<Edge> tree : gg) {
			// check if size == v-1
			if (tree.size() != vertices.size() - 1) {
				ggremove.add(tree);
			} else {
				// reset vertices data
				for (Vertex v : vertices) {
					v.inDegree = 0;
					v.parent = null;
					v.connections.clear();
					v.inLatency = Integer.MAX_VALUE;
					v.level = -1;
					v.colored = false;
					v.inReach.clear();
					v.resCapacity = v.capacity;
				}

				// setup to check validity
				for (Edge edge : tree) {
					edge.from.colored = true;
					edge.to.colored = true;
					edge.to.inDegree += 1;
				}
				// check if all vertices are present, that there is only one root and that tha
				// in degree is 1 for all (not root)
				boolean root = false;
				Vertex rootV = null;
				boolean trashed = false;
				boolean cover = true;
				for (Vertex v : vertices) {
					cover = cover && v.colored;
					// find root
					if (v.inDegree == 0 && root == false) {
						rootV = v;
						root = true;
					}
					// degree not 1, not a tree
					else if (v.inDegree != 1) {
						ggremove.add(tree);
						trashed = true;
						break;
					}
				}
				// there is no root, invalid tree
				if (root != true) {
					// System.out.println("no root");
					ggremove.add(tree);
					trashed = true;
				}

				// good tree
				if (!trashed && cover) {
					// setup vertices connections
					for (Edge edge : tree) {
						edge.from.connections.add(edge.to);
						edge.to.parent = edge.from;
						edge.to.inLatency = edge.latency;
					}
					// check for loops
					List<Vertex> explored = new LinkedList<>();
					List<Vertex> toexplore = new LinkedList<>();
					toexplore.add(rootV);
					while (!toexplore.isEmpty()) {
						Vertex v = toexplore.remove(0);
						explored.add(v);
						for (Vertex con : v.connections) {
							if (explored.contains(con)) {
								// loop not a tree
								ggremove.add(tree);
								trashed = true;
								break;
							} else {
								toexplore.add(con);
							}
						}
					}

					if (!explored.containsAll(vertices)) {
						ggremove.add(tree);
						trashed = true;
					}
					// valid tree, compute score for load
					if (!trashed) {
						count++;
						// System.out.println("goodtreee");
						// system print the tree levels
						List<Vertex> toprint = new ArrayList<>();
						List<Vertex> toadd = new ArrayList<>();
						toprint.add(rootV);
						// while (!toprint.isEmpty()) {
						//
						// toadd.clear();
						// for (Vertex vertex : toprint) {
						// toadd.addAll(vertex.connections);
						// if (vertex.parent != null) {
						// System.out.print("" + vertex.ID + "(" + vertex.parent.ID + ") ");
						// } else
						// System.out.print("" + vertex.ID + "(null)");
						// }
						// System.out.println();
						// toprint.clear();
						// toprint.addAll(toadd);
						// }

						// compute score
						int score = compute_score(rootV, tree, load, minscore, vertices);
//						System.out.println(score);
						if (score < minscore) {
							minscore = score;
							topTree = tree;
						}
					}
				}
			}
		}

		System.out.println("kset that were trees     " + count);
		
		List<List<Edge>> treeees = generateTrees(vertices, topTree, load);
		
		System.out.println("asdfadsfadsfsad    " + treeees.size());
		
		
		if (topTree != null) {
			
			System.out.println("minscore = " + minscore);
			// print tree on dot file
			PrintWriter writer;
			try {

				String ggq = "digraph G { ";
				System.out.println(topTree);
				for (Edge v : topTree) {

					if (!ggq.contains("" + v.from.ID + "->" + v.to.ID + "\n")) {
						ggq += "" + v.from.ID + "->" + v.to.ID + "\n";
					}
				}

				ggq += "}";
				writer = new PrintWriter("graphs/graph_total.dot", "UTF-8");
				writer.println(ggq);
				writer.close();

			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			
		}

		

	}

	public boolean existsLoad(List<Vertex> dests) {
		for (DestSet s : load) {
			if (s.matchDests(dests)) {
				return true;
			}
		}
		return false;
	}

	public static Vertex lca(List<Vertex> vertices, Vertex root) {

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

	// TODO permutation of size k function, to generate possible trees, or use
	// something like
	// http://research.nii.ac.jp/~uno/papers/isaac96web.pdf or
	// http://www.scielo.br/pdf/pope/v25n2/25707.pdf

	// while building check cost of tree, if already found a better one stop, if
	// possible

	private static void getSubsets(List<Edge> superSet, int k, int idx, List<Edge> current, List<List<Edge>> solution) {
		// successful stop clause
		if (current.size() == k) {
			solution.add(new ArrayList<>(current));
			return;
		}
		// unseccessful stop clause
		if (idx == superSet.size())
			return;
		Edge x = superSet.get(idx);
		current.add(x);
		// TODO check if adding x violates the tree
		// "guess" x is in the subset
		getSubsets(superSet, k, idx + 1, current, solution);
		current.remove(x);
		// "guess" x is not in the subset
		getSubsets(superSet, k, idx + 1, current, solution);
	}

	public static List<List<Edge>> getSubsets(List<Edge> superSet, int k) {
		List<List<Edge>> res = new ArrayList<>();
		getSubsets(superSet, k, 0, new ArrayList<Edge>(), res);
		return res;
	}

	// generate all possible desitations
	public static List<List<Vertex>> getAlldestinations(List<Vertex> vertices) {
		List<List<Vertex>> destinations = new ArrayList<>();
		getgetAlldestinations2(vertices, 0, destinations, new ArrayList<>());
		return destinations;

	}

	private static void getgetAlldestinations2(List<Vertex> vertices, int index, List<List<Vertex>> destinations,
			List<Vertex> previous) {
		if (index >= vertices.size()) {
			return;
		}
		previous.add(vertices.get(index));
		destinations.add(new ArrayList<>(previous));
		// consider vertex
		getgetAlldestinations2(vertices, index + 1, destinations, previous);
		// skip vertex
		previous.remove(vertices.get(index));
		getgetAlldestinations2(vertices, index + 1, destinations, previous);
	}

	// generate all trees, assume connected graph
	public static List<List<Edge>> generateTrees(List<Vertex> vertices, List<Edge> edges, List<DestSet> load) {
		List<List<Edge>> trees = new ArrayList<>();
		for (Vertex root : vertices) {
			List<Vertex> visited = new ArrayList<>();
			List<Vertex> available = new ArrayList<>();
			visited.add(root);
			available.addAll(vertices);
			available.remove(root);
			generateTrees(root, visited, visited, available, new ArrayList<>(), trees, load,
					Integer.MAX_VALUE, vertices);
		}
		return trees;
	}

	public static int generateTrees(Vertex root, List<Vertex> fringe, List<Vertex> visited, List<Vertex> available,
			List<Edge> tree, List<List<Edge>> trees, List<DestSet> load, int bestScore, List<Vertex> vertices) {
		//check performance of current tree //TODO store previous score and compute only new changes
		int score =  compute_score(root, tree, load, bestScore, vertices);
		if(score >= bestbestscore) {
			return Integer.MAX_VALUE;
		}
	
		//visited all nodes, save tree
		if (available.isEmpty()) {
			System.out.println("apleanse    " + score);
			trees.add(tree);
			bestbestscore = score;
			// score
			return score;
//			return;
		}
		//stopped all nodes from growing, but not visited all of them. return
		if (fringe.size() == 0) {
			// dead path
			return Integer.MAX_VALUE;
//			return;
		}

		//add them to fringe or not (not with empty set)
		int nscore = Integer.MAX_VALUE;
		for (Vertex f : fringe) {
			// 1toN children
			for (Vertex child : available) {
				//find vertices connecting fringe node to childrens
				List<Edge> newTree = new ArrayList();
				
				for (Edge e :f.outgoingEdges) {
					if(child.ID == e.to.ID) {
						newTree.add(e);
					}
				}
				newTree.addAll(tree);
				//generate new fringe, available and visited list
				List<Vertex> nFringe = new ArrayList<>(fringe);
				nFringe.add(child);
				List<Vertex> navailable =  new ArrayList<>(available);
				navailable.remove(f);
				navailable.remove(child);
				List<Vertex> nvisited =  new ArrayList<>(visited);
				nvisited.add(f);
				nvisited.add(child);
				if(nscore <  bestScore) {
					bestScore = nscore;
				}
				//f can have more children
				nscore = generateTrees(root, nFringe
						, nvisited, navailable, newTree, trees, load,
						bestScore, vertices);
				if(nscore <  bestScore) {
					bestScore = nscore;
				}
				//f has only current children
				nFringe.remove(f);
				nscore = generateTrees(root, nFringe, nvisited, navailable, newTree, trees, load,
						bestScore, vertices);
				if(nscore <  bestScore) {
					bestScore = nscore;
				}
				//f has no children
				nFringe.remove(child);
				nscore = generateTrees(root, nFringe, visited, available, tree, trees, load,
						bestScore, vertices);
			}
		}
		return bestScore;

	}

	public static int compute_score(Vertex root, List<Edge> tree, List<DestSet> load, int minscore,
			List<Vertex> vertices) {
		int score = 0;
		for (Vertex v : vertices) {
			v.inDegree = 0;
			v.parent = null;
			v.connections.clear();
			v.inLatency = Integer.MAX_VALUE;
			v.level = -1;
			v.colored = false;
			v.inReach.clear();
			v.resCapacity = v.capacity;
		}

		List<Vertex> treevertices = new ArrayList<>();
		treevertices.add(root);
		for (Edge edge : tree) {
			treevertices.add(edge.to);
			edge.from.connections.add(edge.to);
			edge.to.parent = edge.from;
			edge.to.inLatency = edge.latency;
		}

		for (DestSet s : load) {
			// compute only if current tree contains all groups
			if (treevertices.containsAll(s.destinations)) {
				// find lca and lca heigh in tree
				Vertex lca = lca(s.destinations, root);
				int lcaH = lca.getLevel();

				List<Vertex> updated = new ArrayList<>();
				lca.updateLoad(s.load, s.destinations, 1, updated);

				boolean saturated = false;
				for (Vertex v : updated) {
					// check if capacity is not saturated
					if (v.resCapacity <= 0) {
						System.out.println("saturated!!!!");
						return Integer.MAX_VALUE;
					}
				}

				if (!saturated) {
					for (Vertex v : s.destinations) {

						// compute score for load on destination set
						score += (v.latecyToLCA(lca) + (v.getLevel() - lcaH) * (s.load / s.destinations.size()));
						// if already worst stop computing
						if (score >= minscore) {
							return Integer.MAX_VALUE;
						}
					}
					// if already worst stop computing
					if (score >= minscore) {
						return Integer.MAX_VALUE;
					}
				}
			}

		}

		return score;
	}

}
