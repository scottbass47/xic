package edu.cornell.cs.cs4120.ir;

import java.util.Set;

import edu.cornell.cs.cs4120.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.ir.visit.IRContainsExprWithSideEffect;
import edu.cornell.cs.cs4120.ir.visit.IRContainsMemSubexprDecorator;
import edu.cornell.cs.cs4120.ir.visit.IRVisitor;
import edu.cornell.cs.cs4120.ir.visit.Lowerer;
import edu.cornell.cs.cs4120.ir.visit.Tiler;
import edu.cornell.cs.cs4120.ir.visit.UnusedLabelVisitor;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import mtm68.assem.JumpAssem;
import mtm68.assem.JumpAssem.JumpType;
import mtm68.assem.operand.Loc;

/**
 * An intermediate representation for a transfer of control
 */
public class IRJump extends IRStmt {
    private IRExpr target;

    /**
     *
     * @param expr the destination of the jump
     */
    public IRJump(IRExpr expr) {
        target = expr;
    }

    public IRExpr target() {
        return target;
    }

    @Override
    public String label() {
        return "JUMP";
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        IRExpr expr = (IRExpr) v.visit(this, target);

        if (expr != target) return v.nodeFactory().IRJump(expr);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(target));
        return result;
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("JUMP");
        target.printSExp(p);
        p.endList();
    }

	@Override
	public IRNode lower(Lowerer v) {
		return v.prependSideEffectsToStmt(this, target.getSideEffects());
	}
	
	@Override
	public IRNode unusedLabels(UnusedLabelVisitor v) {
		v.addLabelsInUse(((IRName)target).name());
		return this;
	}
	
	@Override
	public IRNode tile(Tiler t) {
		IRName targetName = (IRName) target;
		return copyAndSetAssem(new JumpAssem(JumpType.JMP, new Loc(targetName.name())));
	}

	@Override
	public Set<IRExpr> genAvailableExprs() {
		return target.genAvailableExprs();
	}
	
	@Override
	public Set<IRTemp> use() {
		return target.use();
	}

	@Override
	public boolean containsExpr(IRExpr expr) {
		return target.containsExpr(expr);
	}

	@Override
	public IRNode replaceExpr(IRExpr toReplace, IRExpr replaceWith) {
		IRExpr newExpr = (IRExpr)target.replaceExpr(toReplace, replaceWith);

		IRJump copy = copy();
		copy.target = newExpr;
		return copy;
	}

	@Override
	public IRNode decorateContainsMutableMemSubexpr(IRContainsMemSubexprDecorator irContainsMemSubexpr) {
		IRJump copy = copy();
		copy.setContainsMutableMemSubexpr(target.doesContainsMutableMemSubexpr());
		return copy;
	}

	@Override
	public IRNode decorateContainsExprWithSideEffect(IRContainsExprWithSideEffect irContainsExprWithSideEffect) {
		IRJump copy = copy();
		copy.setContainsExprWithSideEffect(target.doesContainsExprWithSideEffect());
		return copy;
	}
}
