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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {
	public List<Vertex> vertices = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public List<DestSet> load = new ArrayList<>();

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
					if (line.contains("%")) {
						str = new StringTokenizer(line, "%");
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
							DestSet s = new DestSet(loadp, ver);
							load.add(s);
						}
					} else {
						str = new StringTokenizer(line, " ");
						// vertex declaration (group)
						if (str.countTokens() == 3) {
							Vertex v = new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(),
									Integer.valueOf(str.nextToken()));
							vertices.add(v);
						}
						// edge declaration
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

							edges.add(e1);
							edges.add(e2);
						}

					}
				}

			}
			fr.close();
			rd.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (rd != null) {
				try {
					rd.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		// generate all possible destinations
		List<Integer> tmp = new ArrayList<>();
		int arr[] = new int[vertices.size()];
		int i = 0;
		for (Vertex v : vertices) {
			arr[i] = v.ID;
			i++;
		}
		int n = arr.length;
		int N = (int) Math.pow(2d, Double.valueOf(n));
		for (int i1 = 1; i1 < N; i1++) {
			String code = Integer.toBinaryString(N | i1).substring(1);
			for (int j = 0; j < n; j++) {
				if (code.charAt(j) == '1') {
					tmp.add(arr[j]);
				}
			}
			if (!existsLoad(tmp)) {
				List<Vertex> ggCode = new ArrayList<>();
				for (Vertex vertex : vertices) {
					if (tmp.contains(vertex.ID)) {
						ggCode.add(vertex);
					}
				}
				load.add(new DestSet(1, ggCode));
			}
			tmp.clear();
		}

		System.out.println("RIP " + load.size() + " " + N);
		Set<Set<Vertex>> ggff = Sets.powerSet(ImmutableSet.copyOf(vertices));

		System.out.println("RIP2 " + ggff.size() + " " + N);

		System.out.println(vertices.size());

		// best possible tree
		int minscore = Integer.MAX_VALUE;
		Set<Edge> topTree = null;

		// generate all possible combination of edges (change to generate only the ones
		// of size v-1)
		load.sort(new DestSet(0, null));
		Set<Edge> e = ImmutableSet.copyOf(edges);
		Set<Set<Edge>> gg = Sets.powerSet(e);
		Set<Set<Edge>> ggremove = Sets.newHashSet();
		for (Set<Edge> tree : gg) {
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
					edge.a.colored = true;
					edge.b.colored = true;
					edge.b.inDegree += 1;
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
					System.out.println("no root");
					ggremove.add(tree);
					trashed = true;
				}

				// good tree
				if (!trashed && cover) {
					// setup vertices connections
					for (Edge edge : tree) {
						edge.a.connections.add(edge.b);
						edge.b.parent = edge.a;
						edge.b.inLatency = edge.latency;
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
						System.out.println("goodtreee");
						// system print the tree levels
						List<Vertex> toprint = new ArrayList<>();
						List<Vertex> toadd = new ArrayList<>();
						toprint.add(rootV);
						while (!toprint.isEmpty()) {

							toadd.clear();
							for (Vertex vertex : toprint) {
								toadd.addAll(vertex.connections);
								if (vertex.parent != null) {
									System.out.print("" + vertex.ID + "(" + vertex.parent.ID + ")    ");
								} else
									System.out.print("" + vertex.ID + "(null)");
							}
							System.out.println();
							toprint.clear();
							toprint.addAll(toadd);
						}

						// compute score
						int score = 0;

						for (DestSet s : load) {
							// find lca and lca heigh in tree
							Vertex lca = lca(s.destinations, rootV);
							int lcaH = lca.getLevel();

							for (Vertex v : s.destinations) {
								// update load on vertex in destination //TODO do just once
								v.updateLoad(s.percentage / s.destinations.size(), lca.ID);
								// check if capacity is not saturated
								if (v.resCapacity < 0) {
									System.out.println("saturated!!!!");
									score = Integer.MAX_VALUE;
									break;
								}
								// compute score for load on destination set
								score += (v.latecyToLCA(lca)
										+ (v.getLevel() - lcaH) * (s.percentage / s.destinations.size()));
								// if already worst stop computing
								if (score >= minscore) {
									break;
								}
							}
							// if already worst stop computing
							if (score >= minscore) {
								break;
							}

						}
						// check if better tree and save
						System.out.println("score  for tree  = " + score);
						if (score < minscore) {
							minscore = score;
							topTree = tree;
						}
					}
				}
			}
		}

		// print tree on dot file
		PrintWriter writer;
		try {

			String ggq = "digraph G { ";
			System.out.println(topTree);
			for (Edge v : topTree) {

				if (!ggq.contains("" + v.a.ID + "->" + v.b.ID + "\n")) {
					ggq += "" + v.a.ID + "->" + v.b.ID + "\n";
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

	public boolean existsLoad(List<Integer> dests) {
		for (DestSet s : load) {
			if (s.matchDests(dests)) {
				return true;
			}
		}
		return false;
	}

	public Vertex lca(List<Vertex> vertices, Vertex root) {

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

}
