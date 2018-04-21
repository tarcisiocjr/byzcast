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
//	public List<Vertex> vertices = new ArrayList<>();
//	public List<Edge> edges = new ArrayList<>();
//	public List<DestSet> load = new ArrayList<>();
//	public int numerOfReplicas = 4;
//	public static BigInteger bestbestscore = BigInteger.ZERO;
//
//	// instead have a sorter for each destination combo
//	public static void main(String[] args) {
//		new Graph("config/load.conf");
//	}
//
//	public Graph(String configFile) {
//
//		// parse edges, vertices, load and constrains
//		FileReader fr;
//		BufferedReader rd = null;
//
//		try {
//			fr = new FileReader(configFile);
//
//			rd = new BufferedReader(fr);
//			String line = null;
//			while ((line = rd.readLine()) != null) {
//				// comment line
//				if (!line.startsWith("#") && !line.isEmpty()) {
//					StringTokenizer str;
//					// load line
//					if (line.contains("m/s")) {
//						str = new StringTokenizer(line, "m/s");
//						if (str.countTokens() == 2) {
//							int loadp = Integer.valueOf(str.nextToken());
//							Set<Vertex> ver = new HashSet<>();
//							str = new StringTokenizer(str.nextToken(), " ");
//							while (str.hasMoreTokens()) {
//								int id = Integer.valueOf(str.nextToken());
//								for (Vertex v : vertices) {
//									if (v.ID == id) {
//										ver.add(v);
//										break;
//									}
//								}
//							}
//							// create destination load
//							DestSet s = new DestSet(loadp, ver);
//							load.add(s);
//						}
//					} else {
//						// vertex declaration (group)
//						str = new StringTokenizer(line, " ");
//						if (str.countTokens() == 3) {
//							Vertex v = new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(),
//									Integer.valueOf(str.nextToken()), numerOfReplicas);
//							vertices.add(v);
//						}
//						// edge declaration latency
//						else if (str.countTokens() == 4) {
//							int a = Integer.valueOf(str.nextToken());
//							str.nextToken(); // drop "-"
//							int b = Integer.valueOf(str.nextToken());
//							int latency = Integer.valueOf(str.nextToken());
//							Vertex aa = null, bb = null;
//							for (Vertex v : vertices) {
//								if (a == v.ID) {
//									aa = v;
//								}
//								if (b == v.ID) {
//									bb = v;
//								}
//							}
//							if (aa == null || bb == null) {
//								System.err.println("connection not know for edge");
//								return;
//							}
//							// edges are bidirectional
//							Edge e1 = new Edge(aa, bb, latency);
//							Edge e2 = new Edge(bb, aa, latency);
//							aa.outgoingEdges.add(e1);
//							bb.outgoingEdges.add(e2);
//
//							edges.add(e1);
//							edges.add(e2);
//						}
//
//					}
//				}
//
//			}
//			fr.close();
//			rd.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			if (rd != null) {
//				try {
//					rd.close();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//			}
//		}
//
//		// testing generate tree
//
////		 int ogsize = vertices.size();
////		 for (int i = vertices.size()-1; i < 5; i++) {
////		 vertices.add(new Vertex(i, "", 100000, numerOfReplicas));
////		 }
//		 for (Vertex v1 : vertices) {
//		 for (Vertex v2 : vertices) {
//		 v1.capacity = 100000;
//		 v1.resCapacity = 100000;
//		 v2.capacity = 100000;
//		 v2.resCapacity = 100000;
//		 if (v1 != v2) {
//		 Edge e = new Edge(v1, v2, 100);
//		 v1.outgoingEdges.add(e);
//		 edges.add(e);
//		 }
//		 }
//		 }
//		System.out.println("done generating test");
//
//		// generate all possible destinations, not all might be specified, and assign
//		// base load (1m/s)ß
//		int baseload = 1;
//		// generate all dests and add not specified ones
//		Set<Set<Vertex>> allDests = getAlldestinations(vertices);
//		System.out.println("done generating dests");
//		Random r = new Random();
//		System.out.println(allDests.size());
//		System.out.println(load.size());
//		// for(List<Vertex> f : allDests) {
//		// for (Vertex v : f) {
//		// System.out.print(v.ID+ " ,");
//		// }
//		// System.out.println();
//		// }
//		for (DestSet d : load) {
//			Set<Vertex> toremove = null;
//			for (Set<Vertex> f : allDests) {
//				if (f.containsAll(d.destinations)) {
//					toremove = f;
//					// System.out.println("fasljdfdksajfkljadslkfjladskfjlask");
//					break;
//				}
//			}
//			allDests.remove(toremove);
//		}
//
//		for (Set<Vertex> d : allDests) {
//			// if (!existsLoad(d)) {
//
//			// load.add(new DestSet(r.nextInt(5)+1, d));
//			load.add(new DestSet(baseload, d));
//			// }
//		}
//		System.out.println("sets dest size = " + load.size());
//		System.out.println("vert size = " + vertices.size());
//		System.out.println("edges size = " + edges.size());
//
//		// find best possible tree
//		int minscore = Integer.MAX_VALUE;
//		List<Edge> topTree = null;
//
//		// build base tree;
//
//		// use
//
//		long start = System.currentTimeMillis(), end;
//		System.out.println(" generating tree1 " + start);
//		// generate all possible combination of edges (change to generate only the ones
//		// of size v-1)
//		System.out.println("generating tree       " );
//		load.sort(new DestSet(0, null));
//		List<List<Edge>> gg = getSubsets(edges, vertices.size() - 1);
//		List<List<Edge>> trees = new ArrayList<>();
//		List<List<Edge>> ggremove = new ArrayList<>();
//		// List<List<Edge>>gg = new ArrayList<>();
//
//		System.out.println("All kset to explore       " + gg.size());
//		int count = 0;
//		for (List<Edge> tree : gg) {
//			// check if size == v-1
//			if (tree.size() != vertices.size() - 1) {
//				ggremove.add(tree);
//			} else {
//				// reset vertices data
//				for (Vertex v : vertices) {
//					v.inDegree = 0;
//					v.parent = null;
//					v.connections.clear();
//					v.inLatency = Integer.MAX_VALUE;
//					v.level = -1;
//					v.colored = false;
//					v.inReach.clear();
//					v.resCapacity = v.capacity;
//				}
//
//				// setup to check validity
//				for (Edge edge : tree) {
//					edge.from.colored = true;
//					edge.to.colored = true;
//					edge.to.inDegree += 1;
//				}
//				// check if all vertices are present, that there is only one root and that tha
//				// in degree is 1 for all (not root)
//				boolean root = false;
//				Vertex rootV = null;
//				boolean trashed = false;
//				boolean cover = true;
//				for (Vertex v : vertices) {
//					cover = cover && v.colored;
//					// find root
//					if (v.inDegree == 0 && root == false) {
//						rootV = v;
//						root = true;
//					}
//					// degree not 1, not a tree
//					else if (v.inDegree != 1) {
//						ggremove.add(tree);
//						trashed = true;
//						break;
//					}
//				}
//				// there is no root, invalid tree
//				if (root != true) {
//					// System.out.println("no root");
//					ggremove.add(tree);
//					trashed = true;
//				}
//
//				// good tree
//				if (!trashed && cover) {
//					// setup vertices connections
//					for (Edge edge : tree) {
//						edge.from.connections.add(edge.to);
//						edge.to.parent = edge.from;
//						edge.to.inLatency = edge.latency;
//					}
//					// check for loops
//					List<Vertex> explored = new ArrayList<>();
//					List<Vertex> toexplore = new ArrayList<>();
//					toexplore.add(rootV);
//					while (!toexplore.isEmpty()) {
//						Vertex v = toexplore.remove(0);
//						explored.add(v);
//						for (Vertex con : v.connections) {
//							if (explored.contains(con)) {
//								// loop not a tree
//								ggremove.add(tree);
//								trashed = true;
//								break;
//							} else {
//								toexplore.add(con);
//							}
//						}
//					}
//
//					if (!explored.containsAll(vertices)) {
//						ggremove.add(tree);
//						trashed = true;
//					}
//					// valid tree, compute score for load
//					if (!trashed) {
//						trees.add(tree);
//						count++;
//						// System.out.println("goodtreee");
//						// system print the tree levels
//						List<Vertex> toprint = new ArrayList<>();
//						List<Vertex> toadd = new ArrayList<>();
//						toprint.add(rootV);
//						// while (!toprint.isEmpty()) {
//						//
//						// toadd.clear();
//						// for (Vertex vertex : toprint) {
//						// toadd.addAll(vertex.connections);
//						// if (vertex.parent != null) {
//						// System.out.print("" + vertex.ID + "(" + vertex.parent.ID + ") ");
//						// } else
//						// System.out.print("" + vertex.ID + "(null)");
//						// }
//						// System.out.println();
//						// toprint.clear();
//						// toprint.addAll(toadd);
//						// }
//
//						// compute score
//						int score = compute_score(rootV, tree, load, minscore, vertices, 0, new ArrayList<>());
//						// System.out.println(score);
//						// System.out.println(score);
//						if (score < minscore) {
//							minscore = score;
//							topTree = tree;
//						}
//					}
//				}
//			}
//		}
//
//		System.out.println("kset that were trees     " + count);
//		System.out.println("minscore      " + minscore);
//		end = System.currentTimeMillis();
//		System.out.println("done tree1  " + (end - start));
//		
//		System.out.println("asdf   " + trees.size());
//		List<List<Edge>> dups = new ArrayList<>();
//		for (List<Edge> te : trees) {
//			if (!dups.contains(te)) {
//				for (List<Edge> te2 : trees) {
//					if (te != te2) {
//						if (te.containsAll(te2)) {
//							dups.add(te2);
//							// System.out.println("dup");
//						}
//					}
//				}
//			}
//		}
//		System.out.println("nodups  " + (trees.size() - dups.size()));
//
//		start = System.currentTimeMillis();
////		System.out.println(" generating tree2 " + start);
////		List<List<Edge>> treeees = generateTrees(vertices, edges, load);
////		end = System.currentTimeMillis();
////		System.out.println("done tree2    " + (end - start));
////
////		System.out.println("asdfadsfadsfsad    " + treeees.size());
////		System.out.println("BESTSGJHYGDUYGSJHSKHS    " + bestbestscore);
////		List<List<Edge>> dups = new ArrayList<>();
////
////		while (!treeees.isEmpty()) {
////			List<Edge> check = treeees.remove(0);
////			dups.add(check);
////			while (treeees.contains(check)) {
////				treeees.remove(check);
////			}
////		}
////		int dsaf = 0;
////		for (List<Edge> list : dups) {
////			PrintWriter writer;
////			try {
////
////				String ggq = "digraph G { ";
//////				System.out.println(topTree);
////				for (Edge v : list) {
////
////					if (!ggq.contains("" + v.from.ID + "->" + v.to.ID + "\n")) {
////						ggq += "" + v.from.ID + "->" + v.to.ID + "\n";
////					}
////				}
////
////				ggq += "}";
////				writer = new PrintWriter("graphs/graph_totassl" + dsaf + ".dot", "UTF-8");
////				dsaf++;
////				writer.println(ggq);
////				writer.close();
////
////			} catch (FileNotFoundException e1) {
////				e1.printStackTrace();
////			} catch (UnsupportedEncodingException e1) {
////				e1.printStackTrace();
////			}
////		}
//		// for (List<Edge> tr : treeees) {
//		//// System.out.println("treesize " + tr.size());
//		// if (!dups.contains(tr)) {
//		// for (List<Edge> tr2 : treeees) {
//		// if (tr!= tr2 & tr2.containsAll(tr)) {
//		//// System.out.println("dups");
//		// dups.add(tr2);
//		// }
//		// }
//		// }
//
//		// }
////		System.out.println("asdfadsfaddsfdssfsad    " + (dups.size()));
////
////		if (!treeees.isEmpty()) {
////			topTree = treeees.get(0);
////
////		}
//		if (topTree != null) {
//
//			System.out.println("minscore = " + minscore);
//			// print tree on dot file
//			PrintWriter writer;
//			try {
//
//				String ggq = "digraph G { ";
//				System.out.println(topTree);
//				for (Edge v : topTree) {
//
//					if (!ggq.contains("" + v.from.ID + "->" + v.to.ID + "\n")) {
//						ggq += "" + v.from.ID + "->" + v.to.ID + "\n";
//					}
//				}
//
//				ggq += "}";
//				writer = new PrintWriter("graphs/graph_totassl.dot", "UTF-8");
//				writer.println(ggq);
//				writer.close();
//
//			} catch (FileNotFoundException e1) {
//				e1.printStackTrace();
//			} catch (UnsupportedEncodingException e1) {
//				e1.printStackTrace();
//			}
//
//		}
//
//	}
//
//	public boolean existsLoad(Set<Vertex> dests) {
//		for (DestSet s : load) {
//			if (s.matchDests(dests)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public static Vertex lca(Set<Vertex> vertices, Vertex root) {
//
//		// tree only has one path between any two nodes, so only one child of root could
//		// be ancestor
//		Vertex ancestor = root;
//		boolean reachable = true;
//		while (reachable) {
//			reachable = true;
//			// if you can not go lower in the tree return current acestor
//			if (ancestor.connections.isEmpty()) {
//				return ancestor;
//			}
//			// check if any of the current ancestor's childrens can reach all destinations
//			for (Vertex v : ancestor.connections) {
//				reachable = true;
//				for (Vertex target : vertices) {
//					// check child reach for all destinations
//					reachable = reachable & v.inReach(target.ID);
//					if (!reachable) {
//						break;
//					}
//				}
//				// if child can reach all it is the new ancestor
//				if (reachable) {
//					// tree only one path between two vertices, so if found lower anchestor it is
//					// not needed to keep searching other children
//					ancestor = v;
//					break;
//				}
//			}
//		}
//		return ancestor;
//	}
//
//	// TODO permutation of size k function, to generate possible trees, or use
//	// something like
//	// http://research.nii.ac.jp/~uno/papers/isaac96web.pdf or
//	// http://www.scielo.br/pdf/pope/v25n2/25707.pdf
//
//	// while building check cost of tree, if already found a better one stop, if
//	// possible
//
//	private static void getSubsets(List<Edge> superSet, int k, int idx, List<Edge> current, List<List<Edge>> solution) {
//		// successful stop clause
//		if (current.size() == k) {
//			solution.add(new ArrayList<>(current));
//			return;
//		}
//		// unseccessful stop clause
//		if (idx == superSet.size())
//			return;
//		Edge x = superSet.get(idx);
//		current.add(x);
//		// TODO check if adding x violates the tree
//		// "guess" x is in the subset
//		getSubsets(superSet, k, idx + 1, current, solution);
//		current.remove(x);
//		// "guess" x is not in the subset
//		getSubsets(superSet, k, idx + 1, current, solution);
//	}
//
//	public static List<List<Edge>> getSubsets(List<Edge> superSet, int k) {
//		List<List<Edge>> res = new ArrayList<>();
//		getSubsets(superSet, k, 0, new ArrayList<Edge>(), res);
//		return res;
//	}
//
//	// generate all possible desitations for n = (2^n)-1 kinda scalable
//	public static Set<Set<Vertex>> getAlldestinations(List<Vertex> vertices) {
//		Set<Set<Vertex>> destinations = new HashSet<>();
//		getgetAlldestinations2(vertices, 0, destinations, new HashSet<>());
//		return destinations;
//	}
//
//	private static void getgetAlldestinations2(List<Vertex> vertices, int index, Set<Set<Vertex>> destinations,
//			Set<Vertex> previous) {
//		if (index >= vertices.size()) {
//			return;
//		}
//		previous.add(vertices.get(index));
//		destinations.add(new HashSet<>(previous));
//		// consider vertex
//		getgetAlldestinations2(vertices, index + 1, destinations, previous);
//		// skip vertex
//		previous.remove(vertices.get(index));
//		getgetAlldestinations2(vertices, index + 1, destinations, previous);
//	}
//
//	public static long start;
//
//	// generate all trees, assume connected graph
//	public static List<Set<Edge>> generateTrees(List<Vertex> vertices, List<Edge> edges, List<DestSet> load) {
//		List<Set<Edge>> trees = new ArrayList<>();
//		bestbestscore = BigInteger.ZERO;
//		start = System.currentTimeMillis();
//		Set<Set<Vertex>> possible = new HashSet<>();
//		for (DestSet d : load) {
//			possible.add(d.destinations);
//		}
//
//		for (Vertex root : vertices) {
//			List<Vertex> visited = new ArrayList<>();
//			List<Vertex> available = new ArrayList<>();
//			visited.add(root);
//			available.addAll(vertices);
//			available.remove(root);
////			generateTrees(root, visited, visited, available, new HashSet<>(), trees, load, Integer.MAX_VALUE,
////					vertices, 0, new ArrayList<>(), possible);
//		}
//		System.out.println(bestbestscore + " alkdsjfkadsjfjhdfkajsdfadsfadsf");
//		return trees;
//	}
//
////	//good
////	public static int generateTrees(Vertex root, List<Vertex> fringe, List<Vertex> visited, List<Vertex> available,
////			Set<Edge> tree, List<Set<Edge>> trees, List<DestSet> load, int bestScore, List<Vertex> vertices,
////			int prevscore, List<Edge> prevTree, List<List<Vertex>> possibilities) {
////		// check performance of current tree //TODO store previous score and compute
////		// only new changes
////		if ((System.currentTimeMillis() - start) > 2 * 1000) {
////			start = System.currentTimeMillis();
////			System.out.println(bestbestscore);
////		}
////		// int score = compute_score(root, tree, load, bestbestscore, vertices,
////		// prevscore, prevTree);
////		// if (score >= bestbestscore || score >= bestScore) {
////		//// System.out.println("prune " + bestbestscore + " " + bestScore + " " +
////		// score);
////		// return Integer.MAX_VALUE;
////		// }
////		// 
////		// // visited all nodes, save tree
////		if (available.isEmpty() && tree.size() == vertices.size() - 1) {
////			bestbestscore = bestbestscore.add(BigInteger.ONE);
////
////			// System.out.println("apleanse " + score);
////			trees.add(tree);
////			// bestbestscore = score;
////			// // score
////			// return score;
////			return 1;
////		}
////		// stopped all nodes from growing, but not visited all of them. return
////		if (fringe.size() == 0) {
////			// dead path
////			return Integer.MAX_VALUE;
////			// return;
////		}
////
////		// add them to fringe or not (not with empty set)
////		int nscore = Integer.MAX_VALUE;
////		for (Vertex f : fringe) {
////			// 1toN children
////			// for (Vertex child : available) {
////			for (List<Vertex> child : possibilities) {
////				if(available.containsAll(child)) {
////				// find vertices connecting fringe node to childrens
////				List<Edge> newTree = new ArrayList();
////				for (Vertex c : child) {
////					for (Edge e : f.outgoingEdges) {
////						if (c.ID == e.to.ID) {
////							newTree.add(e);
////						}
////					}
////				}
////				newTree.addAll(tree);
////				// generate new fringe, available and visited list
////				List<Vertex> nFringe = new ArrayList<>(fringe);
////				nFringe.addAll(child);
////				List<Vertex> navailable = new ArrayList<>(available);
////				navailable.remove(f);
////				navailable.removeAll(child);
//////				navailable.remove(child);
////				Set<Vertex> nvisited = new ArrayList<>(visited);
////				nvisited.add(f);
////				nvisited.addAll(child);
////
////				// f can have more children
////				nscore = generateTrees(root, nFringe, nvisited, navailable, newTree, trees, load, bestScore, vertices,
////						3, tree, possibilities);
////
////				// f has only current children
////				nFringe.remove(f);
////				nscore = generateTrees(root, nFringe, nvisited, navailable, newTree, trees, load, bestScore, vertices,
////						3, tree, possibilities);
////
////				// f has no children
////				navailable = new ArrayList<>(available);
////				navailable.remove(f);
////				nFringe.removeAll(child);
////				nvisited.removeAll(child);
////				nscore = generateTrees(root, nFringe, nvisited, navailable, tree, trees, load, bestScore, vertices, 3,
////						prevTree, possibilities);
////			}
////			}
////		}
////		return bestScore;
////
////	}
//
//	public static int compute_score(Vertex root, List<Edge> tree, List<DestSet> load, int minscore,
//			List<Vertex> vertices, int prevScore, List<Edge> prevTree) {
//		int score = prevScore;
//		for (Vertex v : vertices) {
//			v.inDegree = 0;
//			v.parent = null;
//			v.connections.clear();
//			v.inLatency = Integer.MAX_VALUE;
//			v.level = -1;
//			v.colored = false;
//			v.inReach.clear();
//			v.resCapacity = v.capacity;
//		}
//
//		List<Vertex> treevertices = new ArrayList<>();
//		List<Vertex> oldtreevertices = new ArrayList<>();
//		treevertices.add(root);
//		oldtreevertices.add(root);
//		for (Edge edge : tree) {
//			if (prevTree.contains(edge)) {
//				oldtreevertices.add(edge.to);
//			}
//			treevertices.add(edge.to);
//			edge.from.connections.add(edge.to);
//			edge.to.parent = edge.from;
//			edge.to.inLatency = edge.latency;
//		}
//
//		for (DestSet s : load) {
//			// compute only if current tree contains all groups
//			// if (treevertices.containsAll(s.destinations) &&
//			// !oldtreevertices.containsAll(s.destinations)) {
//			// find lca and lca heigh in tree
//			Vertex lca = lca(s.destinations, root);
//			int lcaH = lca.getLevel();
//
//			List<Vertex> updated = new ArrayList<>();
//			lca.updateLoad(s.load, s.destinations, 1, updated);
//
//			boolean saturated = false;
//			for (Vertex v : updated) {
//				// check if capacity is not saturated
//				if (v.resCapacity <= 0) {
//					System.out.println("saturated!!!!");
//					return Integer.MAX_VALUE;
//				}
//			}
//
//			if (!saturated) {
//				for (Vertex v : s.destinations) {
//
//					// compute score for load on destination set
//					score += (v.latecyToLCA(lca) + (v.getLevel() - lcaH) * (s.load / s.destinations.size()));
//					// if already worst stop computing
//					if (score >= minscore) {
//						return Integer.MAX_VALUE;
//					}
//				}
//				// if already worst stop computing
//				if (score >= minscore) {
//					return Integer.MAX_VALUE;
//				}
//			}
//		}
//
//		// }
//
//		return score;
//	}

}
