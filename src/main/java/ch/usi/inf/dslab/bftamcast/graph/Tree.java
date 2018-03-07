/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class Tree implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1340477045756411763L;
	private Vertex root;
	private List<Integer> destinations;

	/**
	 * Main for testing
	 * 
	 * @param args
	 *            none
	 */
	public static void main(String[] args) {
		new Tree("config/tree.conf",0);
	}

	/**
	 * getter for list of destinations in the tree
	 * 
	 * @return the field destinations containing the id of all destinations in the
	 *         tree
	 */
	public List<Integer> getDestinations() {
		return destinations;
	}

	/**
	 * Constructor
	 * 
	 * @param configFile
	 *            containing the id of the vertices and their config path for bft
	 *            smart and connection between them
	 */
	public Tree(String configFile, int proxyID) {
		
		destinations = new ArrayList<>();
		List<Vertex> vertices = new ArrayList<>();
		FileReader fr;
		BufferedReader rd;
		try {
			fr = new FileReader(configFile);
			rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				System.out.println(line);
				if (!line.startsWith("#") && !line.isEmpty()) {
					// TODO instead of reading nodes and then tree, read nodes and specs (througput
					// etc and build optimal tree)
					StringTokenizer str = new StringTokenizer(line, " ");
					if (str.countTokens() == 2) {
						// throw away in config file (temporary to distinguish between vertex
						// declaration and edges)
						vertices.add(new Vertex(Integer.valueOf(str.nextToken()), str.nextToken(), proxyID));
						destinations.add(vertices.get(vertices.size() - 1).groupId);
					}
					if (str.countTokens() == 3) {

						int from = Integer.valueOf(str.nextToken());
						str.nextToken();// throw away "->"
						int to = Integer.valueOf(str.nextToken());

						for (Vertex v1 : vertices) {
							if (v1.groupId == from) {
								for (Vertex v2 : vertices) {
									if (v2.groupId == to) {
										v1.children.add(v2);
										v1.childernIDs.add(v2.groupId);
										v2.parent = v1;
									}
								}
							}
						}

						for (Vertex v : vertices) {
							if (v.parent == null) {
								root = v;
							}
						}
					}
				}
			}

			fr.close();
			rd.close();
			
			
			
			System.out.println("");
			System.out.println("");
			System.out.println("ID          "+proxyID);
			System.out.println("");
			System.out.println("");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param vetices
	 *            list of vertices you are interest into knowing their lowest common
	 *            ancestor
	 * @return the lowest common ancestor in the tree of all the vertices in the
	 *         input vertices list.
	 */
	public Vertex lca(int[] ids) {

		List<Vertex> vertices = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			vertices.add(findVertexById(ids[i]));
		}
		// List<List<Vertex>> ancestors = new ArrayList<>();
		// for (Vertex v : vertices) {
		// List<Vertex> vAncestors = new ArrayList();
		// Vertex tmp = v;
		// while(v != null) {
		// vAncestors.add(tmp);
		// tmp = tmp.parent;
		// }
		// ancestors.add(vAncestors);
		// }

		// tree only has one path between any two nodes, so only one child of root could
		// be anchestor
		Vertex ancestor = root;
		boolean reachable = true;
		while (reachable) {
			reachable = true;
			System.out.println(ancestor.groupId);
			if(ancestor.children.isEmpty()) {
				return ancestor;
			}
			for (Vertex v : ancestor.children) {
				reachable = true;
				for (Vertex target : vertices) {
					reachable = reachable & v.inReach(target.groupId);
					if (!reachable) {
						break;
					}
				}
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

	public Vertex findVertexById(int id) {
		return root.findVertexByID(id);
	}

}
