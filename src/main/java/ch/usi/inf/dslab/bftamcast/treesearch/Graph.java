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
import java.util.List;
import java.util.StringTokenizer;

import ch.usi.inf.dslab.bftamcast.treesearch.Vertex;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {
	public List<Vertex> vertices = new ArrayList<>();
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
		System.out.println("RIP " + load.size() + " " + N);




		load.sort(new DestSet(0, null));
		//generate all possible trees
		//evaluate based on all dests and minimize score
		//		https://blogs.msdn.microsoft.com/ericlippert/2010/04/22/every-tree-there-is/

		for (

		DestSet s : load) {
			//check best root with score  =  for n dests: load *lca - get eight * load/n
			//
//			try to have all scores as low as possible
			
		}

//		PrintWriter writer;
//		try {
//			for (DestSet s : load) {
//				String gg = "digraph G { ";
//				String d = "";
//				for (Vertex v : s.destinations) {
//					d += v.ID + "_";
//					if (true) {
//						if (!gg.contains("" + s.root.ID + "->" + v.ID + "\n")) {
//							gg += "" + s.root.ID + "->" + v.ID + "\n";
//							v.printed = true;
//						}
//					}
//				}
//				gg += "}";
//
//				writer = new PrintWriter("graphs/graph_" + d + ".dot", "UTF-8");
//				writer.println(gg);
//				writer.close();
//			}
//
//			String gg = "digraph G { ";
//			for (Vertex v : vertices) {
//				for (Vertex d : v.connections) {
//					if (!gg.contains("" + v.ID + "->" + d.ID + "\n")) {
//						gg += "" + v.ID + "->" + d.ID + "\n";
//					}
//				}
//			}
//
//			gg += "}";
//			writer = new PrintWriter("graphs/graph_total.dot", "UTF-8");
//			writer.println(gg);
//			writer.close();
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

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
	
	
public int lca(List<Vertex> vertices, Vertex root) {

		// tree only has one path between any two nodes, so only one child of root could
		// be ancestor
	int level = 0;
		Vertex ancestor = root;
		boolean reachable = true;
		while (reachable) {
			reachable = true;
			//if you can not go lower in the tree return current acestor
			if(ancestor.connections.isEmpty()) {
				return level;
			}
			//check if any of the current ancestor's childrens can reach all destinations
			for (Vertex v : ancestor.connections) {
				reachable = true;
				for (Vertex target : vertices) {
					//check child reach for all destinations
					reachable = reachable & v.inReach(target.ID);
					if (!reachable) {
						break;
					}
				}
				//if child can reach all it is the new ancestor
				if (reachable) {
					// tree only one path between two vertices, so if found lower anchestor it is
					// not needed to keep searching other children
					ancestor = v;
					level +=1;
					break;
				}
			}
		}
		return level;
	}

}
