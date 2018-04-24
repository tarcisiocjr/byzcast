///**
// * 
// */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<Load> load = new ArrayList<>();
	public int numerOfReplicas = 4;
	public static long bestbestscore = Long.MAX_VALUE;

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
							Set<Vertex> ver = new HashSet<>();
							str = new StringTokenizer(str.nextToken(), " ");
							while (str.hasMoreTokens()) {
								int id = Integer.valueOf(str.nextToken());
								for (Vertex v : vertices) {
									if (v.getID() == id) {
										ver.add(v);
										break;
									}
								}
							}
							// create destination load
							Load s = new Load(loadp, ver);
							load.add(s);
						}
					} else {
						// vertex declaration (group)
						str = new StringTokenizer(line, " ");
						if (str.countTokens() == 3) {
							Vertex v = new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(),
									Integer.valueOf(str.nextToken()), numerOfReplicas);
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
								if (a == v.getID()) {
									aa = v;
								}
								if (b == v.getID()) {
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
							aa.addEdge(e1);
							bb.addEdge(e2);

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

		// testing generate tree

		// int ogsize = vertices.size();
		// for (int i = vertices.size()-1; i < 5; i++) {
		// vertices.add(new Vertex(i, "", 100000, numerOfReplicas));
		// }
		for (Vertex v1 : vertices) {
			for (Vertex v2 : vertices) {
				// v1.capacity = 100000;
				// v1.resCapacity = 100000;
				// v2.capacity = 100000;
				// v2.resCapacity = 100000;
				if (v1 != v2) {
					Edge e = new Edge(v1, v2, 100);
					v1.addEdge(e);
					edges.add(e);
				}
			}
		}
		System.out.println("done generating test");

		// generate all possible destinations, not all might be specified, and assign
		// base load (1m/s)ß
		int baseload = 1;
		// generate all dests and add not specified ones
		Set<Set<Vertex>> allDests = getAlldestinations(vertices);
		System.out.println("done generating dests");
		Random r = new Random();
		System.out.println(allDests.size());
		System.out.println(load.size());
		// for(List<Vertex> f : allDests) {
		// for (Vertex v : f) {
		// System.out.print(v.ID+ " ,");
		// }
		// System.out.println();
		// }
		for (Load d : load) {
			Set<Vertex> toremove = null;
			for (Set<Vertex> f : allDests) {
				if (f.containsAll(d.destinations)) {
					toremove = f;
					// System.out.println("fasljdfdksajfkljadslkfjladskfjlask");
					break;
				}
			}
			allDests.remove(toremove);
		}

		for (Set<Vertex> d : allDests) {
			// if (!existsLoad(d)) {

			// load.add(new DestSet(r.nextInt(5)+1, d));
			load.add(new Load(baseload, d));
			// }
		}
		System.out.println("sets dest size = " + load.size());
		System.out.println("vert size = " + vertices.size());
		System.out.println("edges size = " + edges.size());

		// find best possible tree
		long minscore = Integer.MAX_VALUE;
		List<Edge> topTree = null;

		// build base tree;

		// use

		long start = System.currentTimeMillis(), end;
		System.out.println(" generating tree1 " + start);
		// generate all possible combination of edges (change to generate only the ones
		// of size v-1)
		System.out.println("generating tree       ");
		load.sort(new Load(0, null));
		List<List<Edge>> gg = getSubsets(edges, vertices.size() - 1);
		List<List<Edge>> trees = new ArrayList<>();
		List<List<Edge>> ggremove = new ArrayList<>();
		// List<List<Edge>>gg = new ArrayList<>();

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
				// check if all vertices are present, that there is only one root and that that
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
					List<Vertex> explored = new ArrayList<>();
					List<Vertex> toexplore = new ArrayList<>();
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
						trees.add(tree);
						// System.out.println("goodtreee");
						// system print the tree levels

						// compute score

						long score = compute_score(rootV, tree, load, minscore, vertices, 0, new ArrayList<>());

						if (score < minscore) {
							System.out.println(score);
							System.out.println();
							minscore = score;
							topTree = tree;
						}
					}
				}
			}
		}

		System.out.println("kset that were trees     " + count);
		System.out.println("minscore      " + minscore);
		end = System.currentTimeMillis();
		System.out.println("done tree1  " + (end - start));
		Graph2.printTree(topTree,-122);

		start = System.currentTimeMillis();

	}

	public boolean existsLoad(Set<Vertex> dests) {
		for (Load s : load) {
			if (s.matchDests(dests)) {
				return true;
			}
		}
		return false;
	}

	public static Vertex lca(Set<Vertex> vertices, Vertex root) {

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

	// generate all possible desitations for n = (2^n)-1 kinda scalable
	public static Set<Set<Vertex>> getAlldestinations(List<Vertex> vertices) {
		Set<Set<Vertex>> destinations = new HashSet<>();
		getgetAlldestinations2(vertices, 0, destinations, new HashSet<>());
		return destinations;
	}

	private static void getgetAlldestinations2(List<Vertex> vertices, int index, Set<Set<Vertex>> destinations,
			Set<Vertex> previous) {
		if (index >= vertices.size()) {
			return;
		}
		previous.add(vertices.get(index));
		destinations.add(new HashSet<>(previous));
		// consider vertex
		getgetAlldestinations2(vertices, index + 1, destinations, previous);
		// skip vertex
		previous.remove(vertices.get(index));
		getgetAlldestinations2(vertices, index + 1, destinations, previous);
	}

	public static long compute_score(Vertex root, List<Edge> tree, List<Load> load, long minscore,
			List<Vertex> vertices, long prevScore, List<Edge> prevTree) {
		long score = 0;
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
		List<Vertex> oldtreevertices = new ArrayList<>();
		treevertices.add(root);
		oldtreevertices.add(root);
		for (Edge edge : tree) {
			if (prevTree.contains(edge)) {
				oldtreevertices.add(edge.to);
			}
			treevertices.add(edge.to);
			edge.from.connections.add(edge.to);
			edge.to.parent = edge.from;
			edge.to.inLatency = edge.latency;
		}

		for (Load s : load) {
			// compute only if current tree contains all groups
			if (treevertices.containsAll(s.destinations)) {// &&
				// !oldtreevertices.containsAll(s.destinations)) {
				// find lca and lca heigh in tree
				Vertex lca = lca(s.destinations, root);
				int lcaH = lca.getLevel();

				List<Vertex> updated = new ArrayList<>();
				lca.updateLoad(s.load, s.destinations, 1, updated);

				boolean saturated = false;
				for (Vertex v : updated) {
					// check if capacity is not saturated
					if (v.resCapacity <= 0) {
//						System.out.println("saturated!!!!");
						return Long.MAX_VALUE;
					}
				}

				if (!saturated) {
					for (Vertex v : s.destinations) {

						// compute score for load on destination set
						score += (v.latecyToLCA(lca) + (v.getLevel() - lcaH) * (s.load / s.destinations.size()));
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

}
