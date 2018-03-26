/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
							DestSet s = new DestSet(Integer.valueOf(str.nextToken()));
							str = new StringTokenizer(str.nextToken(), " ");
							while (str.hasMoreTokens()) {
								int id = Integer.valueOf(str.nextToken());
								for (Vertex v : vertices) {
									if (v.ID == id) {
										s.destinations.add(v);
										s.destinationsIDS.add(v.ID);
										break;
									}
								}
							}
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

		for (DestSet s : load) {
			System.out.print(s.percentage + "% ");
			for (Vertex v : s.destinations) {
				System.out.print(v.ID + " ");
			}
			System.out.println();
		}

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

		for (DestSet s : load) {
			if (!s.handled) {
				List<Vertex> possibleRoots = new ArrayList();
				possibleRoots.addAll(s.destinations);
				for (DestSet overlap : s.overlaps) {
					if (overlap.handled) {
						possibleRoots.add(overlap.root);
					} else {
						possibleRoots.removeAll(overlap.destinations);
						possibleRoots.addAll(overlap.destinations);
					}
				}
				Vertex maxCapacityVertex = null;
				int max = 0;
				for (Vertex v : possibleRoots) {
					if (v.resCapacity > max) {
						max = v.resCapacity;
						maxCapacityVertex = v;
					}
				}
				s.root = maxCapacityVertex;
				s.handled = true;
				for (DestSet overlap : s.overlaps) {
					if (!overlap.handled) {
						overlap.root = maxCapacityVertex;
						overlap.handled = true;
					}
				}
				// TODO if another load overlaps either use it's root or use common node as root
				// for current load or
				maxCapacityVertex.resCapacity -= maxCapacityVertex.capacity * (s.percentage / 100);
			}
		}

		System.out.println(getRoot(new ArrayList<Integer>() {
			{
				add(1);
				add(3);
				add(2);
			}
		}).ID);
		System.out.println(getRoot(new ArrayList<Integer>() {
			{
				add(4);
				add(5);
				add(2);
			}
		}).ID);
		System.out.println(getRoot(new ArrayList<Integer>() {
			{
				add(8);
				add(3);
				add(2);
			}
		}).ID);
		
		System.out.println(getRoot(new ArrayList<Integer>() {
			{
				add(8);
			}
		}).ID);
		
		System.out.println(getRoot(new ArrayList<Integer>() {
			{
				add(0);
				add(1);
				add(2);
				add(3);
				add(4);
				add(5);
				add(6);
				add(7);
				add(8);
				add(9);
			}
		}).ID);

	}

	public Vertex getRoot(List<Integer> dests) {
		for (DestSet s : load) {
			if (s.matchDests(dests)) {
				return s.root;
			}
		}

		int max = 0;
		Vertex maxV = null;
		for (Vertex v : vertices) {
			if (dests.contains(v.ID) && v.resCapacity < max) {
				max = v.resCapacity;
				maxV = v;
			}
		}

		return maxV;
	}

}
