/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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

		// print destinations and loads %

		for (DestSet s : load) {
			System.out.print(s.percentage + "% ");
			for (Vertex v : s.destinations) {
				System.out.print(v.ID + " ");
			}
			System.out.println();
		}

		// find overlapping sets of destinations (find prefix order)

		for (DestSet s : load) {
			for (DestSet s2 : load) {
				if (s2 != s && !Collections.disjoint(s.destinationsIDS, s2.destinationsIDS)) {
					if (!s2.overlaps.contains(s)) {
						s2.overlaps.add(s);
					}
					if (!s.overlaps.contains(s2)) {
						s.overlaps.add(s2);
					}
				}
			}

		}

		// find optimal genuine node to sort for each destination set
		for (DestSet s : load) {

			Vertex maxCapacityVertex = null;
			double max = 0;
			for (Vertex v : s.destinations) {
				if (v.resCapacity > max) {
					max = v.resCapacity;
					maxCapacityVertex = v;
				}
			}
			s.root = maxCapacityVertex;
			s.handled = true;
			System.out
					.println("1111MAX = " + maxCapacityVertex.ID + " res capacity = " + maxCapacityVertex.resCapacity);

			// TODO if another load overlaps either use it's root or use common node as
			// root
			// for current load or external if genuine are overloaded
			maxCapacityVertex.resCapacity = maxCapacityVertex.resCapacity
					- (maxCapacityVertex.capacity * (s.percentage / 100.0));

		}
		
		//handle prefix order for overlapping

		for (DestSet s : load) {
			// genuine for single target
			if (!s.handled) {
				List<Vertex> possibleRoots = new ArrayList<>();
				possibleRoots.add(s.root);
				for (DestSet overlap : s.overlaps) {
					if (overlap.handled) {
						possibleRoots.add(overlap.root);
					} else {
						for(Vertex v : overlap.destinations) {
							if(s.destinations.contains(v)) {
								possibleRoots.remove(v);
								possibleRoots.add(v);
							}
						}
					}
				}
				Vertex maxCapacityVertex = null;
				double max = 0;
				for (Vertex v : possibleRoots) {
					if (v.resCapacity > max) {
						max = v.resCapacity;
						maxCapacityVertex = v;
					}
				}
				s.root = maxCapacityVertex;
				s.handled = true;
				System.out
						.println("MAX = " + maxCapacityVertex.ID + " res capacity = " + maxCapacityVertex.resCapacity);

				// TODO if another load overlaps either use it's root or use common node as
				// root
				// for current load or external if genuine are overloaded
				maxCapacityVertex.resCapacity = maxCapacityVertex.resCapacity
						- (maxCapacityVertex.capacity * (s.percentage / 100.0));
			}
		}

		for (DestSet s : load) {
			getRoot(s.destinationsIDS);
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

}
