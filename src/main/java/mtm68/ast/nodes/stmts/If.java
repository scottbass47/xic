package mtm68.ast.nodes.stmts;

import java.util.Optional;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import mtm68.ast.nodes.Expr;
import mtm68.ast.nodes.Node;
import mtm68.visit.TypeChecker;
import mtm68.visit.Visitor;

public class If extends Statement {
	
	private Expr condition;
	private Statement ifBranch;
	private Optional<Statement> elseBranch;

	public If(Expr condition, Statement ifBranch) {
		this.condition = condition;
		this.ifBranch = ifBranch;
		this.elseBranch = Optional.empty();
	}

	public If(Expr condition, Statement ifBranch, Statement elseBranch) {
		this.condition = condition;
		this.ifBranch = ifBranch;
		this.elseBranch = Optional.of(elseBranch);
	}

	@Override
	public String toString() {
		return "If [condition=" + condition + ", ifBranch=" + ifBranch + ", elseBranch=" + elseBranch + "]";
	}

	@Override
	public void prettyPrint(SExpPrinter p) {
		p.startList();
		p.printAtom("if");
		condition.prettyPrint(p);
		if(elseBranch.isPresent()) {
			ifBranch.prettyPrint(p);
			elseBranch.get().prettyPrint(p);
		}
		else {
			ifBranch.prettyPrint(p);
		}
		p.endList();
	}

	public Expr getCondition() {
		return condition;
	}

	public Statement getIfBranch() {
		return ifBranch;
	}

	public Optional<Statement> getElseBranch() {
		return elseBranch;
	}

	@Override
	public Node visitChildren(Visitor v) {
		Expr newCondition = condition.accept(v);
		Statement newIfBranch = ifBranch.accept(v);
		Statement newElseBranch = elseBranch.isPresent() ? elseBranch.get().accept(v) : null;
		
		if(newCondition != condition
				|| newIfBranch != ifBranch
				|| (elseBranch.isPresent() && elseBranch.get() != newElseBranch)) {
			return new If(newCondition, newIfBranch, newElseBranch);
		} else {
			return this;
		}
	}

	@Override
	public Node typeCheck(TypeChecker tc) {
		// TODO Auto-generated method stub
		return null;
	}
}
