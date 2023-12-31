package edu.cornell.cs.cs4120.ir.visit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.cornell.cs.cs4120.ir.IRCJump;
import edu.cornell.cs.cs4120.ir.IRCallStmt;
import edu.cornell.cs.cs4120.ir.IRCompUnit;
import edu.cornell.cs.cs4120.ir.IRFuncDefn;
import edu.cornell.cs.cs4120.ir.IRJump;
import edu.cornell.cs.cs4120.ir.IRLabel;
import edu.cornell.cs.cs4120.ir.IRNode;
import edu.cornell.cs.cs4120.ir.IRNodeFactory;
import edu.cornell.cs.cs4120.ir.IRNodeFactory_c;
import edu.cornell.cs.cs4120.ir.IRSeq;

/**
 * Visitor class responsible for removing unused labels.
 * 
 * @author Scott
 */
public class UnusedLabelVisitor extends IRVisitor {

	private Map<String, IRLabel> labelMap;
	private Set<String> labelsInUse;

	public UnusedLabelVisitor() {
		this(new IRNodeFactory_c());
	}
	
	public UnusedLabelVisitor(IRNodeFactory inf) {
		super(inf);
		
		labelMap = new HashMap<String, IRLabel>();
		labelsInUse = new HashSet<>();
	}
	
	/**
	 * Record the existence of <code>label</code>
	 * @param label
	 */
	public void recordLabel(IRLabel label) {
		labelMap.put(label.name(), label);
	}
	
	/** 
	 * Mark <code>labels</code> to be in use.
	 * @param labels
	 */
	public void addLabelsInUse(String...labels) {
		List<String> filtered = Arrays.stream(labels)
				.filter(l -> l != null)
				.collect(Collectors.toList());

		labelsInUse.addAll(filtered);
	}
	
	@Override
	protected IRVisitor enter(IRNode parent, IRNode n) {
		if(n instanceof IRSeq) {
			labelMap.clear();
			labelsInUse.clear();
		}

		return this;
	}
	
	/**
	 * Sets the <code>used</code> property on each IR label according
	 * to whether or not it's been detected to be in use.
	 */
	public void markUnusedLabels() {
		Set<String> labelsToKeep = new HashSet<>(labelMap.keySet());
		labelsToKeep.retainAll(labelsInUse);

		for(String labelToKeep : labelsToKeep) {
			labelMap.get(labelToKeep).setUsed(true);
		}
	}
	
	@Override
	protected IRNode override(IRNode parent, IRNode n) {
		if(!shouldVisit(n)) return n;

		return null;
	}
	
	private boolean shouldVisit(IRNode n) {
		return n instanceof IRJump 
				|| n instanceof IRCJump
				|| n instanceof IRCallStmt
				|| n instanceof IRLabel
				|| n instanceof IRSeq
				|| n instanceof IRCompUnit
				|| n instanceof IRFuncDefn;
	}
	
	@Override
	protected IRNode leave(IRNode parent, IRNode n, IRNode n_, IRVisitor v_) {
		return n_.unusedLabels(this);
	}
}