/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.direct.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ch.usi.inf.dslab.bftamcast.direct.client.ConsoleClientDirect;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class TreeDirect implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1340477045756411763L;
	private VertexDirect root;
	private List<Integer> destinations;

	/**
	 * Main for testing
	 * 
	 * @param args
	 *            none
	 */
	public static void main(String[] args) {
		new TreeDirect("config/tree.conf", 0, null);
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
	public TreeDirect(String configFile, int proxyID, ConsoleClientDirect c) {

		destinations = new ArrayList<>();
		List<VertexDirect> vertices = new ArrayList<>();
		FileReader fr;
		BufferedReader rd;
		try {
			fr = new FileReader(configFile);
			rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				if (!line.startsWith("#") && !line.isEmpty()) {
					// etc and build optimal tree)
					StringTokenizer str = new StringTokenizer(line, " ");
					// vertex declaration (group)
					if (str.countTokens() == 2) {
						vertices.add(new VertexDirect(Integer.valueOf(str.nextToken()), configFile.replace("tree.conf", "") + str.nextToken(), proxyID, c));
						destinations.add(vertices.get(vertices.size() - 1).getGroupId());
					}
					// connection declaration
					if (str.countTokens() == 3) {
						int from = Integer.valueOf(str.nextToken());
						str.nextToken();// throw away "->"
						int to = Integer.valueOf(str.nextToken());

						// add connections in vertices
						for (VertexDirect v1 : vertices) {
							if (v1.getGroupId() == from) {
								for (VertexDirect v2 : vertices) {
									if (v2.getGroupId() == to) {
										v1.getChildren().add(v2);
										v1.getChildernIDs().add(v2.getGroupId());
										v2.setParent(v1);
									}
								}
							}
						}

						// the vertex with no parent is the root vertex of the tree
						for (VertexDirect v : vertices) {
							if (v.getParent() == null) {
								root = v;
							}
						}
					}
				}
			}

			fr.close();
			rd.close();
		} catch (Exception e) {
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
	public VertexDirect lca(int[] ids) {
		if(ids.length == 1) {
			return findVertexById(ids[0]);
		}

		
		List<VertexDirect> vertices = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			vertices.add(findVertexById(ids[i]));
		}

		// tree only has one path between any two nodes, so only one child of root could
		// be ancestor
		VertexDirect ancestor = root;
		boolean reachable = true;
		while (reachable) {
			reachable = true;
			//if you can not go lower in the tree return current acestor
			if(ancestor.getChildren().isEmpty()) {
				return ancestor;
			}
			//check if any of the current ancestor's childrens can reach all destinations
			for (VertexDirect v : ancestor.getChildren()) {
				reachable = true;
				for (VertexDirect target : vertices) {
					//check child reach for all destinations
					reachable = reachable & v.inReach(target.getGroupId());
					if (!reachable) {
						break;
					}
				}
				//if child can reach all it is the new ancestor
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

	/**
	 * find vertex in tree from it's id
	 * 
	 * @param id
	 * @return vertex with specified id or null
	 */
	public VertexDirect findVertexById(int id) {
		return root.findVertexByID(id);
	}

}
