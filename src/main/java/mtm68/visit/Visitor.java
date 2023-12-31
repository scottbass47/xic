package mtm68.visit;

import mtm68.ast.nodes.Node;

public abstract class Visitor {

	public Visitor enter(Node parent, Node n) {
		return this;
	}
	
	public abstract Node leave(Node parent, Node n);
}
