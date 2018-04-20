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
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.text.ChangedCharSetException;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph2 {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<DestSet> load = new ArrayList<>();
	public int numerOfReplicas = 4;
	public static BigInteger bestbestscore = BigInteger.ZERO;

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

		// testing generate tree

		// int ogsize = vertices.size();
		// for (int i = vertices.size()-1; i < 4; i++) {
		// vertices.add(new Vertex(i, "", 100000, numerOfReplicas));
		// }
		for (Vertex v1 : vertices) {
			for (Vertex v2 : vertices) {
				v1.capacity = 100000;
				v1.resCapacity = 100000;
				v2.capacity = 100000;
				v2.resCapacity = 100000;
				if (v1 != v2) {
					Edge e = new Edge(v1, v2, 100);
					v1.outgoingEdges.add(e);
					edges.add(e);
				}
			}
		}
		System.out.println("done generating test");

		// generate all possible destinations, not all might be specified, and assign
		// base load (1m/s)ß
		int baseload = 1;
		// generate all dests and add not specified ones
		List<List<Vertex>> allDests = getAlldestinations(vertices);
		System.out.println("done generating dests");
		Random r = new Random();
		// for(List<Vertex> f : allDests) {
		// for (Vertex v : f) {
		// System.out.print(v.ID+ " ,");
		// }
		// System.out.println();
		// }
		for (DestSet d : load) {
			List<Vertex> toremove = null;
			for (List<Vertex> f : allDests) {
				if (f.containsAll(d.destinations)) {
					toremove = f;
					// System.out.println("fasljdfdksajfkljadslkfjladskfjlask");
					break;
				}
			}
			allDests.remove(toremove);
		}

		for (List<Vertex> d : allDests) {
			// if (!existsLoad(d)) {

			// load.add(new DestSet(r.nextInt(5)+1, d));
			load.add(new DestSet(baseload, d));
			// }
		}
		System.out.println("sets dest size = " + load.size());
		System.out.println("vert size = " + vertices.size());
		System.out.println("edges size = " + edges.size());

		load.sort(new DestSet(0, null));

		List<Edge> tZero = new ArrayList<>(vertices.get(0).outgoingEdges);

		List<List<Edge>> trees = new ArrayList<>();

		List<List<Vertex>> sets = getAlldestinations(vertices);
		// sets.add(new ArrayList<>());

		 for(List<Vertex> set : sets) {
		 System.out.println();
		 System.out.print( "set : ");
		 for(Vertex v : set) {
		 System.out.print(v.ID + " ");
		 }
		 }
		System.out.println();
		for (Vertex root : vertices) {

			// check dups in set
			// List<List<Vertex>> dups2 = new ArrayList<>();
			// for (List<Vertex> te : sets) {
			// if (!dups2.contains(te)) {
			// for (List<Vertex> te2 : sets) {
			// if (te != te2) {
			// if (te.containsAll(te2) && te2.containsAll(te)) {
			// dups2.add(te2);
			//// System.out.println("dup");
			// }
			// }
			// }
			// }
			// }
			// System.out.println("dupkdfasahkjdshfs " + dups2.size());
			List<Vertex> vi = new ArrayList<>();
			List<List<Vertex>> prevselected = new ArrayList<>();

			vi.add(root);
			List<Vertex> fr1 = new ArrayList<>();
			fr1.add(root);
			prevselected.add(new ArrayList<>(fr1));
			gen(new ArrayList<>(), vi, trees, vertices.size(), sets, fr1);

		}

		System.out.println("asdf   " + trees.size());
		List<List<Edge>> dups = new ArrayList<>();
		for (List<Edge> te : trees) {
			if (!dups.contains(te)) {
				for (List<Edge> te2 : trees) {
					if (te != te2) {
						if (te.containsAll(te2)) {
							dups.add(te2);
							// System.out.println("dup");
						}
					}
				}
			}
		}
		System.out.println("nodups  " + (trees.size() - dups.size()));

	}

	public static int iteration = 0;

	public List<List<Edge>> gexp(List<Vertex> vertices, List<Edge> edges) {
		List<List<Edge>> trees = new ArrayList<>();

		for (Vertex root : vertices) {
			
		}

		return trees;
	}
	
//	public List<List<Edge>> gdf (Vertex root, List<>){
//		List<Edge> tree = new ArrayList<>();
//
//		for (Edge e : edges) {
//			e.used = false;
//		}
//		List<Vertex> visited = new ArrayList<>();
//		visited.add(root);
//		List<Vertex> available = new ArrayList<>(visited);
//		ddf(visited, available, tree);
//
//		if (tree.size() == vertices.size() - 1) {
//
//			for (Edge e : tree) {
//				Edge dead = e;
//				for (Edge e1 : edges) {
//					e1.used = false;
//				}
//				e.used = true;
//				
//			}
//		}
//	}

	public void ddf(List<Vertex> visited, List<Vertex> available, List<Edge> tree) {
		for (Vertex v : available) {
			for (Edge e : v.outgoingEdges) {
				if (!e.used && !visited.contains(e.to)) {
					visited.add(e.to);
					List<Vertex> av = new ArrayList<>(available);
					av.remove(v);
					av.add(e.to);
					ddf(visited, av, tree);
					return;

				}
			}
		}

	}

	public void gen(List<Edge> tree, List<Vertex> visited, List<List<Edge>> trees, int numVertices,
			List<List<Vertex>> sets, List<Vertex> fringe) throws Exception {
		//TODO fix duplicates: 
		
		//      0
		//    1    2
		//
		//add 4 to 1; add 3 to 2 return; return; add 3 to 2; add 4 to 1
		//
		

		iteration++;
		System.out.println("iteration  " + iteration);

		PrintWriter writer;
		try {

			String ggq = "digraph G { ";
			for (Edge v : tree) {

				if (!ggq.contains("" + v.from.ID + "->" + v.to.ID + "\n")) {
					ggq += "" + v.from.ID + "->" + v.to.ID + "\n";
				}
			}

			ggq += "}";
			writer = new PrintWriter("graphs/graph_totassl" + iteration + ".dot", "UTF-8");
			writer.println(ggq);
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		if (tree.size() == numVertices - 1) {
			boolean exist = false;

			 for (List<Edge> te : trees) {
			 if (te.containsAll(tree)) {
			 exist = true;
			 System.out.println("dup dup " + iteration);
			 throw (new Exception());
			 }
			 }
			if (true)
				trees.add(new ArrayList<>(tree));
			System.out.println("iteration " + iteration + "done");
			return;
		}

		for (Vertex v : fringe) {
			for (List<Vertex> chance : sets) {
				boolean good = true;
				for (Vertex vertex : visited) {
					if (chance.contains(vertex)) {
						good = false;
						break;
					}
				}
				if (good) {

					List<Vertex> nv = new ArrayList<>(visited);
					nv.addAll(chance);

					List<Vertex> nf = new ArrayList<>(fringe);
					nf.remove(v);

					List<Edge> nt = new ArrayList<>(tree);

					System.out.print("adding to: " + v.ID + " vertices : ");
					for (Edge e : v.outgoingEdges) {
						if (chance.contains(e.to)) {
							System.out.print(e.to.ID + " ");
							nf.add(e.to);
							nt.add(e);
						}
					}
					System.out.println();
					// if (nt.size() == numVertices - 1) {
					// boolean a = false;
					// for (int i = trees.size() - 1; i >= 0; i--) {
					// List<Edge> tt = trees.get(i);
					// if (tt.containsAll(tree)) {
					// a = true;
					// break;
					// }
					// }
					// if (!a) {
					// gen(nt, nv, trees, numVertices, sets, nf);
					// }
					// } else {
					gen(nt, nv, trees, numVertices, sets, nf);
					// }

				}
			}
		}
//		System.out.println("iteration " + iteration + "done");
		// System.out.println("done recurs");

	}

	//
	// public void gen2(List<Vertex> visited, List<Vertex> fringe, int graphsize,
	// List<List<Edge>> trees,
	// List<Edge> tree) {
	// if (tree.size() == graphsize - 1) {
	// trees.add(new ArrayList<>(tree));
	// return;
	// }
	//
	// for (Vertex v : fringe) {
	// for (Edge e : v.outgoingEdges) {
	// if (!visited.contains(e.to)) {
	// List<Vertex> nvisited = new ArrayList<>(visited);
	// List<Vertex> nfringe = new ArrayList<>(fringe);
	// List<Edge> ntree = new ArrayList<>(tree);
	//
	// nvisited.add(e.to);
	// nfringe.add(e.to);
	// ntree.add(e);
	//
	// gen2(nvisited, nfringe, graphsize, trees, ntree);
	// nfringe.remove(v);
	// // stop
	// gen2(nvisited, nfringe, graphsize, trees, ntree);
	//
	// }
	// }
	// }
	// }
	//
	// public List<Edge> nonbackedges(List<Edge> tree, List<Vertex> vertices,
	// List<Edge> edges) {
	// List<Edge> tosort = new ArrayList<>(edges);
	// tosort.removeAll(tree);
	// List<Edge> nonbackedges = new ArrayList<>();
	//
	// for (Vertex v : vertices) {
	// v.connections.clear();
	// v.parent = null;
	// v.inReach.clear();
	// }
	// for (Edge e : tree) {
	// Vertex v = e.from;
	// Vertex v2 = e.to;
	// v.connections.add(v2);
	// v2.parent = v;
	// }
	//
	// for (Edge e : tosort) {
	// if (e.to.inReach(e.from.ID)) {
	// // backedge }else {
	//
	// } else {
	// nonbackedges.add(e);
	// }
	// }
	//
	// return nonbackedges;
	//
	// }
	//
	// public boolean existsLoad(List<Vertex> dests) {
	// for (DestSet s : load) {
	// if (s.matchDests(dests)) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// public void explore(List<Edge> tree, List<Edge> noback, List<List<Edge>>
	// trees, List<Vertex> vertices,
	// List<Edge> edges) {
	// if (noback.isEmpty()) {
	// return;
	// }
	// for (Edge e : noback) {
	// List<Edge> ntree = new ArrayList<>(tree);
	// List<Edge> nnoback = new ArrayList<>(noback);
	// nnoback.remove(e);
	// for (Edge ee : ntree) {
	// if (ee.to == e.to) {
	// ntree.remove(ee);
	// ntree.add(e);
	// break;
	// }
	// }
	// for (List<Edge> t : trees) {
	// if (t.containsAll(ntree)) {
	// return;
	// }
	// }
	// trees.add(ntree);
	//
	// explore(ntree, nonbackedges(ntree, vertices, edges), trees, vertices, edges);
	// }
	// }
	//
	// public static Vertex lca(List<Vertex> vertices, Vertex root) {
	//
	// // tree only has one path between any two nodes, so only one child of root
	// could
	// // be ancestor
	// Vertex ancestor = root;
	// boolean reachable = true;
	// while (reachable) {
	// reachable = true;
	// // if you can not go lower in the tree return current acestor
	// if (ancestor.connections.isEmpty()) {
	// return ancestor;
	// }
	// // check if any of the current ancestor's childrens can reach all
	// destinations
	// for (Vertex v : ancestor.connections) {
	// reachable = true;
	// for (Vertex target : vertices) {
	// // check child reach for all destinations
	// reachable = reachable & v.inReach(target.ID);
	// if (!reachable) {
	// break;
	// }
	// }
	// // if child can reach all it is the new ancestor
	// if (reachable) {
	// // tree only one path between two vertices, so if found lower anchestor it is
	// // not needed to keep searching other children
	// ancestor = v;
	// break;
	// }
	// }
	// }
	// return ancestor;
	// }
	//
	// // generate all possible desitations
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
	//
	// public static int compute_score(Vertex root, List<Edge> tree, List<DestSet>
	// load, int minscore,
	// List<Vertex> vertices, int prevScore, List<Edge> prevTree) {
	// int score = prevScore;
	// for (Vertex v : vertices) {
	// v.inDegree = 0;
	// v.parent = null;
	// v.connections.clear();
	// v.inLatency = Integer.MAX_VALUE;
	// v.level = -1;
	// v.colored = false;
	// v.inReach.clear();
	// v.resCapacity = v.capacity;
	// }
	//
	// List<Vertex> treevertices = new ArrayList<>();
	// List<Vertex> oldtreevertices = new ArrayList<>();
	// treevertices.add(root);
	// oldtreevertices.add(root);
	// for (Edge edge : tree) {
	// if (prevTree.contains(edge)) {
	// oldtreevertices.add(edge.to);
	// }
	// treevertices.add(edge.to);
	// edge.from.connections.add(edge.to);
	// edge.to.parent = edge.from;
	// edge.to.inLatency = edge.latency;
	// }
	//
	// for (DestSet s : load) {
	// // compute only if current tree contains all groups
	// // if (treevertices.containsAll(s.destinations) &&
	// // !oldtreevertices.containsAll(s.destinations)) {
	// // find lca and lca heigh in tree
	// Vertex lca = lca(s.destinations, root);
	// int lcaH = lca.getLevel();
	//
	// List<Vertex> updated = new ArrayList<>();
	// lca.updateLoad(s.load, s.destinations, 1, updated);
	//
	// boolean saturated = false;
	// for (Vertex v : updated) {
	// // check if capacity is not saturated
	// if (v.resCapacity <= 0) {
	// System.out.println("saturated!!!!");
	// return Integer.MAX_VALUE;
	// }
	// }
	//
	// if (!saturated) {
	// for (Vertex v : s.destinations) {
	//
	// // compute score for load on destination set
	// score += (v.latecyToLCA(lca) + (v.getLevel() - lcaH) * (s.load /
	// s.destinations.size()));
	// // if already worst stop computing
	// if (score >= minscore) {
	// return Integer.MAX_VALUE;
	// }
	// }
	// // if already worst stop computing
	// if (score >= minscore) {
	// return Integer.MAX_VALUE;
	// }
	// }
	// }
	//
	// // }
	//
	// return score;
	// }

}
