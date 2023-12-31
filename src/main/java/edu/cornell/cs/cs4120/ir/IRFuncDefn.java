package edu.cornell.cs.cs4120.ir;

import java.util.Set;

import edu.cornell.cs.cs4120.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.ir.visit.IRConstantFolder;
import edu.cornell.cs.cs4120.ir.visit.IRContainsExprWithSideEffect;
import edu.cornell.cs.cs4120.ir.visit.IRContainsMemSubexprDecorator;
import edu.cornell.cs.cs4120.ir.visit.IRVisitor;
import edu.cornell.cs.cs4120.ir.visit.InsnMapsBuilder;
import edu.cornell.cs.cs4120.ir.visit.Lowerer;
import edu.cornell.cs.cs4120.ir.visit.Tiler;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import mtm68.assem.FuncDefnAssem;
import mtm68.assem.ReplaceableReg;
import mtm68.assem.SeqAssem;

/** An IR function definition */
public class IRFuncDefn extends IRNode_c {
    private String name;
    private IRStmt body;
    private int numArgs;

    public IRFuncDefn(String name, IRStmt body, int numArgs) {
        this.name = name;
        this.body = body;
        this.numArgs = numArgs;
    }

    public String name() {
        return name;
    }

    public IRStmt body() {
        return body;
    }
    
    public int numArgs() {
   	 return numArgs;
    }

    @Override
    public String label() {
        return "FUNC " + name;
    }
    
    public IRStmt getBody() {
		return body;
	}

	public void setBody(IRStmt body) {
		this.body = body;
	}

	@Override
    public IRNode visitChildren(IRVisitor v) {
        IRStmt stmt = (IRStmt) v.visit(this, body);

        if (stmt != body) return v.nodeFactory().IRFuncDefn(name, stmt, numArgs);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(body));
        return result;
    }

    @Override
    public InsnMapsBuilder buildInsnMapsEnter(InsnMapsBuilder v) {
        v.addNameToCurrentIndex(name);
        v.addInsn(this);
        return v;
    }

    @Override
    public IRNode buildInsnMaps(InsnMapsBuilder v) {
        return this;
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("FUNC");
        p.printAtom(name);
        body.printSExp(p);
        p.endList();
    }

	@Override
	public IRNode lower(Lowerer v) {
		return this;
	}

	@Override
	public IRNode constantFold(IRConstantFolder v) {
		return this;
	}
	
	@Override
	public IRNode tile(Tiler t) {
		SeqAssem bodyAssem = (SeqAssem)body.getAssem();

		FuncDefnAssem assem = new FuncDefnAssem(name, numArgs, bodyAssem);
		return this.copyAndSetAssem(assem);
	}

	@Override
	public Set<IRExpr> genAvailableExprs() {
		return body.genAvailableExprs();
	}
	
	@Override
	public Set<IRTemp> use() {
		return body.use();
	}

	@Override
	public boolean containsExpr(IRExpr expr) {
		return body.containsExpr(expr);
	}
	
	@Override
	public IRNode decorateContainsMutableMemSubexpr(IRContainsMemSubexprDecorator irContainsMemSubexpr) {
		IRFuncDefn copy = copy();
		copy.setContainsMutableMemSubexpr(body.doesContainsMutableMemSubexpr());
		return copy;
	}

	@Override
	public IRNode decorateContainsExprWithSideEffect(IRContainsExprWithSideEffect irContainsExprWithSideEffect) {
		IRFuncDefn copy = copy();
		copy.setContainsExprWithSideEffect(body.doesContainsExprWithSideEffect());
		return copy;
	}	
	@Override
	public IRNode replaceExpr(IRExpr toReplace, IRExpr replaceWith) {
		IRStmt newBody = (IRStmt)body.replaceExpr(toReplace, replaceWith);
		
		IRFuncDefn copy = copy();
		copy.body = newBody;
		return copy;
	}
}
