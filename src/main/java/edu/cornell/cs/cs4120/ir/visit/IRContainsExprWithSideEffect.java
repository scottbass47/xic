package edu.cornell.cs.cs4120.ir.visit;

import edu.cornell.cs.cs4120.ir.IRNode;
import edu.cornell.cs.cs4120.ir.IRNodeFactory;

public class IRContainsExprWithSideEffect extends IRVisitor {

	public IRContainsExprWithSideEffect(IRNodeFactory inf) {
		super(inf);
	}
	
	@Override
	public IRNode leave(IRNode parent, IRNode n, IRNode n_, IRVisitor v_) {		
		return n_.decorateContainsExprWithSideEffect(this);
	}

}
