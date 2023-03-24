package ch.usi.inf.dslab.byzcast.util;

import ajs.printutils.Color;
import ajs.printutils.PrettyPrintTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */

public class TreeConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(TreeConfiguration.class);

	protected Map<String, String> configs;
	protected static String configHome = "";
	protected static int groupId;
	private int byzcastGroupId;
	private int[] byzcastTreeMembers;
	private String[] byzcastTreeTopology;
	public static Tree<String> tree;
	public static Tree[] c;

	public TreeConfiguration(int[] treeMembers, String[] treeTopology) {
		tree = new Tree<>("0");
		c = new Tree[treeMembers.length];

		for (int tt = 0; tt < treeTopology.length; tt++) {
			logger.debug(treeTopology[tt]);
			String[] sidesOfGraph = treeTopology[tt].split("->");

			// it's the root of tree
			if (Integer.parseInt(sidesOfGraph[0]) == 0) {
				c[Integer.parseInt(sidesOfGraph[1])] = tree.addChild(sidesOfGraph[1]);
				logger.debug("Root node: " + sidesOfGraph[0] + "->" + sidesOfGraph[1]);
			} else {
				if (hasChildren(sidesOfGraph[0], treeTopology)) {
					c[Integer.parseInt(sidesOfGraph[1])] = c[Integer.parseInt(sidesOfGraph[0])].addChild(sidesOfGraph[1]);
				}
//				logger.debug("Children node: " + sidesOfGraph[0] + "->" + sidesOfGraph[1]);
//				c[Integer.parseInt(sidesOfGraph[0])] = c[Integer.parseInt(sidesOfGraph[0])].addChild(sidesOfGraph[1]);
			}
		}

		var pt = new PrettyPrintTree<Tree<String>>(
				Tree::getChildren,
				Tree::getValue
		);
		pt.setColor(Color.BLUE);
		pt.setBorder(true);
		pt.display(tree);
	}

	public static boolean hasChildren(String source, String[] treeTopology1) {
		for (int i = 0; i < treeTopology1.length; i++) {
			String[] sidesOfGraph1 = treeTopology1[i].split("->");
			if (sidesOfGraph1[0].equals(source)) {
				return true;
			}
		}
		return false;
	}

	public TreeConfiguration(int group, String configPath) {
		groupId = group;
		configHome = configPath;
		loadConfig();
		init();

		new TreeConfiguration(getByzcastTreeMembers(), getByzcastTreeTopology());
	}

	public boolean getRouteToGroup(int source, int destination) {
		logger.debug("Buscando rota de : " + source + " Para: " + destination);
		if (isConnected(source, destination)) {
			logger.debug("Encontrei rota de " + source + " para " + destination);
			return true;
		}
		return false;
	}

	private boolean isConnected(int source, int destination) {

		List<Integer> g = new ArrayList<>();
		for (Tree<String> children1 : getChildren(source)) {
			g.add(Integer.valueOf(children1.getValue()));
		}


		for (int i = 0; i < g.size(); i++) {
			if (g.get(i) == destination) {
			logger.debug("g" + g.get(i) + " destination: " + destination);
			return true;
			}

		}

		boolean teste;
		if (g.contains(destination)) {
			return true;
		} else {
			logger.debug("isConnected : " + source + " Para: " + destination);
			for (Integer s : g) {
				if (isConnected(s, destination)) {
					return true;
				}
			}
		}
		return false;
	}
	protected void init() {
		try {
			String s = configs.remove("byzcast.group.tree_members");
			if (s == null) {
				System.err.println("Wrong byzcast.config file format.");
			} else {
				StringTokenizer str = new StringTokenizer(s, ",");
				byzcastTreeMembers = new int[str.countTokens()];
				for (int i = 0; i < byzcastTreeMembers.length; i++) {
					byzcastTreeMembers[i] = Integer.parseInt(str.nextToken());
				}
			}

			s = configs.remove("byzcast.group.tree_topology");
			if (s == null) {
				System.err.println("Wrong byzcast.config file format.");
			} else {
				StringTokenizer str = new StringTokenizer(s, ",");
				byzcastTreeTopology = new String[str.countTokens()];
				for (int i = 0; i < byzcastTreeTopology.length; i++) {
					byzcastTreeTopology[i] = (str.nextToken());
				}
			}
		} catch (Exception e) {
			System.err.println("Wrong byzcast.config file format.");
			e.printStackTrace(System.out);
		}


	}

	private void loadConfig() {
		configs = new Hashtable<String, String>();
		try {
			if (configHome == null || configHome.equals("")) {
				configHome = "config";
			}
			String sep = System.getProperty("file.separator");
			String path = configHome + sep + "byzcast.config";
			;
			FileReader fr = new FileReader(path);
			BufferedReader rd = new BufferedReader(fr);
			String line = null;
			while ((line = rd.readLine()) != null) {
				if (!line.startsWith("#")) {
					StringTokenizer str = new StringTokenizer(line, "=");
					if (str.countTokens() > 1) {
						configs.put(str.nextToken().trim(), str.nextToken().trim());
					}
				}
			}
			fr.close();
			rd.close();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	public int getByzcastGroupId() {
		return groupId;
	}

	public int[] getByzcastTreeMembers() {
		return byzcastTreeMembers;
	}

	public ArrayList<Tree<String>> getChildren(int group) {
		if (group == 0) {
			return tree.getChildren();
		} else {
			return c[group].getChildren();
		}
	}



	public String[] getByzcastTreeTopology() {
		return byzcastTreeTopology;
	}

	public ArrayList<Integer> getGroupsOnMyReach(int group) {
		ArrayList<Integer> groups = new ArrayList<>();
		for (Tree<String> children : getChildren(group)) {
			logger.debug(children.getValue());
			groups.add(Integer.valueOf(children.getValue()));
		}
		return groups;
	}
	

	public int[] getGroupsNotOnMyReach(int group, List<Integer> groupsOnMyReach) {
		// Montando rota para grupos onde não tenho contato direto.
		int[] routeToChildrenNotOnMyReach = new int[getByzcastTreeMembers().length];
		for (int i = 0; i < getByzcastTreeMembers().length; i++) {
			routeToChildrenNotOnMyReach[i] = -1;
			if (i != group) {
				for (Integer children : groupsOnMyReach) {
					if (getRouteToGroup(children, i)) {
						routeToChildrenNotOnMyReach[i] = children;
						logger.debug("routeToChildrenNotOnMyReach[" + i + "] = " + children);
					}
				}
			}
		}
		return routeToChildrenNotOnMyReach;
	}
}
