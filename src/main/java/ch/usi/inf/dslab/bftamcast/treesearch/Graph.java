/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.treesearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ch.usi.inf.dslab.bftamcast.graph.Tree;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Graph {

	public static void main(String[] args) {
		new Graph("config/load.conf");
	}

	public Graph(String configFile) {

		List<Vertex> vertices = new ArrayList<>();
		List<DestSet> load = new ArrayList<>();
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
			Vertex maxCapacityVertex = null;
			int max = 0;
			for (Vertex v : s.destinations) {
				if (v.capacity > max) {
					max = v.capacity;
					maxCapacityVertex = v;
				}
			}
			if (maxCapacityVertex.parent != null) {
				for (Vertex v : s.destinations) {
					if (v == maxCapacityVertex.parent) {
						System.out.println("LOOOP, have to select another max capacity node");
					}
				}
			}
			for (Vertex v : s.destinations) {
				if (v != maxCapacityVertex) {
					if(v.parent != null || v.parent != maxCapacityVertex) {
						System.out.println("new parent change, have to rearrange");
					}
					v.parent = maxCapacityVertex;
					
					if(!maxCapacityVertex.connections.contains(v)) {
						maxCapacityVertex.connections.add(v);
					}
				}
			}
			maxCapacityVertex.capacity = maxCapacityVertex.capacity*(1-s.percentage/100);
			
		}
		
		for(Vertex v : vertices){
			if(v.parent == null) {
				print(v);
			}
		}

	}
	public void print(Vertex v) {
		System.out.print("" + v.ID + " -> ");
		for(Vertex c : v.connections){
			System.out.print(c.ID+", " );
		}
		System.out.println();
		for(Vertex c : v.connections){
			print(c);
		}
	}

}
