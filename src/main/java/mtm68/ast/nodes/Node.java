package mtm68.ast.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.cornell.cs.cs4120.ir.IRExpr;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import java_cup.runtime.ComplexSymbolFactory.Location;
import mtm68.visit.FunctionCollector;
import mtm68.visit.NodeToIRNodeConverter;
import mtm68.visit.TypeChecker;
import mtm68.visit.Visitor;

public abstract class Node implements HasLocation, Cloneable {

	private Location startLoc;

	protected IRExpr irNode;

	public abstract void prettyPrint(SExpPrinter p);

	/**
	 * Accepts a visitor
	 * 
	 * @param v
	 *           the visitor to use
	 * @return the visited node
	 */
	@SuppressWarnings("unchecked")
	public <N extends Node> N accept(Node parent, Visitor v) {
		Visitor v2 = v.enter(parent, this);
		Node n = visitChildren(v2);
		return (N) v2.leave(parent, n);
	}

	public <N extends Node> N accept(Visitor v) {
		return accept(null, v);
	}

	/**
	 * Visit each element of a list.
	 * 
	 * @param l
	 *           The list to visit.
	 * @param v
	 *           The visitor to visit with
	 * @return A new list with each element from the old list replaced by the
	 *         result of visiting that element. If {@code l} is {@code null},
	 *         {@code null} is returned.
	 * 
	 *         This function is adopted from polyglot.ast.Node_c
	 */
	public <N extends Node> List<N> acceptList(List<N> l, Visitor v) {

		if (l == null) {
			return null;
		}

		List<N> result = l;
		List<N> vl = new ArrayList<>(l.size());

		for (N n : l) {
			N n2 = n.accept(v);
			if (n != n2) {
				result = vl;
			}
			// Add everything to vl in case any n != n2
			vl.add(n2);
		}

		return result;
	}

	public <N extends Node> Optional<N> acceptOptional(Node parent, Optional<N> opt, Visitor v) {
		if (!opt.isPresent())
			return opt;

		N node = opt.get();
		N newNode = node.accept(parent, v);

		if (node == newNode)
			return opt;

		return Optional.of(newNode);
	}

	public <N extends Node> Optional<N> acceptOptional(Optional<N> opt, Visitor v) {
		return acceptOptional(null, opt, v);
	}

	/**
	 * Visit all children that belong to {@code this}.
	 * 
	 * @param v
	 *           The visitor to visit with
	 * @return The node returned from visiting all of its children.
	 */
	public abstract Node visitChildren(Visitor v);

	public abstract Node typeCheck(TypeChecker tc);

	public abstract Node convertToIR(NodeToIRNodeConverter cv);

	public Node extractFunctionDecl(FunctionCollector fc) {
		return this;
	}

	public Location getStartLoc() {
		return startLoc;
	}

	public void setStartLoc(Location startLoc) {
		this.startLoc = startLoc;
	}

	@Override
	public int getLine() {
		return startLoc.getLine();
	}

	@Override
	public int getColumn() {
		return startLoc.getColumn();
	}
	
	public IRExpr getIrNode() {
		return irNode;
	}

	public void setIrNode(IRExpr irNode) {
		this.irNode = irNode;
	}

	@SuppressWarnings("unchecked")
	public <N extends Node> N copy() {
		try {
			return (N) clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Copies this Expr, sets the irNode of the copied Expr,
	 * and returns that copied Expr.
	 * @param node the IRNode to set the copied Expr
	 * @return the copied Expr
	 */
	public <E extends Node> E copyAndSetIRNode(IRExpr node) {
		E newE = this.copy();
		newE.setIrNode(node);
		return newE;
	}
}
