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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import ch.usi.inf.dslab.bftamcast.treesearch.Vertex;

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

		FileReader fr;
		BufferedReader rd;

		try {
			fr = new FileReader(configFile);

			rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				if (!line.startsWith("#") && !line.isEmpty()) {
					StringTokenizer str;
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
						} else if (str.countTokens() == 4) {
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
		}

		// generate all possible destinations (to clean up)
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

		// System.out.println("RIP " + load.size() + " " + N);

		System.out.println(vertices.size());

		int minscore = Integer.MAX_VALUE;
		Set<Edge> topTree = null;

		load.sort(new DestSet(0, null));
		Set<Edge> e = ImmutableSet.copyOf(edges);

		// change to generate only sets of size v-1
		Set<Set<Edge>> gg = Sets.powerSet(e);
		Set<Set<Edge>> ggremove = Sets.newHashSet();
		for (Set<Edge> tree : gg) {
			if (tree.size() != vertices.size() - 1) {
				ggremove.add(tree);
			} else {
				for (Vertex v : vertices) {
					v.inDegree = 0;
					v.parent = null;
					v.connections.clear();
					v.inLatency = Integer.MAX_VALUE;
					v.level = -1;
					v.colored = false;
				}

				for (Edge edge : tree) {
					edge.a.colored = true;
					edge.b.colored = true;
					edge.b.inDegree += 1;
				}
				boolean root = false;
				Vertex rootV = null;
				boolean trashed = false;
				boolean cover = true;
				for (Vertex v : vertices) {
					cover = cover && v.colored;
					if (v.inDegree == 0 && root == false) {
						rootV = v;
						root = true;
					} else if (v.inDegree != 1) {
						ggremove.add(tree);
						trashed = true;
						break;
					}
				}
				if (root != true) {
					System.out.println("no root");
					ggremove.add(tree);
					trashed = true;
				}

				// good tree
				if (!trashed && cover) {
					for (Edge edge : tree) {
						edge.a.connections.add(edge.b);
						edge.b.parent = edge.a;
						edge.b.inLatency = edge.latency;
					}
					List<Vertex> explored = new LinkedList();
					List<Vertex> toexplore = new LinkedList();
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
					if (!trashed) {
//
//						PrintWriter writer;
//						try {
//							String ggq = "digraph G { ";
//							System.out.println(tree);
//							for (Edge v : tree) {
//
//								if (!ggq.contains("" + v.a.ID + "->" + v.b.ID + "\n")) {
//									ggq += "" + v.a.ID + "->" + v.b.ID + "\n";
//								}
//							}
//
//							ggq += "}";
//							writer = new PrintWriter("graphs/graph_total" + tree.hashCode() + ".dot", "UTF-8");
//							writer.println(ggq);
//							writer.close();
//
//						} catch (FileNotFoundException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						} catch (UnsupportedEncodingException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
						System.out.println("goodtreee");
						
						List<Vertex> toprint = new ArrayList();
						List<Vertex> toadd = new ArrayList();
						toprint.add(rootV);
						while(!toprint.isEmpty()) {
							
							toadd.clear();
							for (Vertex vertex : toprint) {
								toadd .addAll(vertex.connections);
								if(vertex.parent!= null) {
								System.out.print(""+vertex.ID + "("+vertex.parent.ID+")    ");
								}else
									System.out.print(""+vertex.ID + "(null)");
							}
							System.out.println();
							toprint.clear();
							toprint.addAll(toadd);
							
							
						}

						for (DestSet s : load) {
							// check best root with score = for n dests: load *lca - get eight * load/n
							Vertex lca = lca(s.destinations, rootV);
							int lcaH = lca.getLevel();
							System.out.println("lca height = "+ lcaH + " id=  " + lca.ID);
							int score = 0;
							for (Vertex v : s.destinations) {
								//TODO check load capacity
								score += (v.latecyToLCA(lca)*0
										+ (lcaH - v.getLevel())) ; //(s.percentage / s.destinations.size()));
								if (score >= minscore) {
									break;
								}
							}
							System.out.println("score  for dests : " + s.destinations.toString() + " = " + score);
							if (score < minscore) {
								minscore = score;
								topTree = tree;
							}
						}
					}
				}
			}
		}

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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public Vertex getRoot(List<Integer> dests) {
		System.out.print("List " + dests.toString() + "  has root ");
		for (DestSet s : load) {
			if (s.matchDests(dests)) {
				System.out.println(s.root.ID + " here");
				return s.root;
			}
		}

		double max = 0;
		Vertex maxV = null;
		for (Vertex v : vertices) {
			if (dests.contains(v.ID) && v.resCapacity < max) {
				max = v.resCapacity;
				maxV = v;
			}
		}
		System.out.println(maxV.ID + " there");

		return maxV;
	}

	public boolean existsLoad(List<Integer> dests) {
		for (DestSet s : load) {
			if (s.matchDests(dests)) {
				return true;
			}
		}
		return false;
	}

	public Vertex findParent(Vertex g) {
		for (Vertex v : vertices) {
			if (v.connections.contains(g)) {
				return v;
			}
		}
		return null;
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
	

}




