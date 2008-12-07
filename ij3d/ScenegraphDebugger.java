package ij3d;

import java.util.Enumeration;
import javax.media.j3d.Group;
import javax.media.j3d.Node;

public class ScenegraphDebugger {
	
	public static void displayTree(Node root) {
		displayTree(root, "");
	}

	private static void displayTree(Node node, String indent) {
		System.out.println(indent + node);
		if(node instanceof Group) {
			Enumeration ch = ((Group)node).getAllChildren();
			while(ch.hasMoreElements())
				displayTree((Node)ch.nextElement(), indent + "   ");
		}
	}
}
