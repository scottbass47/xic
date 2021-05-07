package mtm68.assem.cfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import mtm68.assem.Assem;
import mtm68.assem.JumpAssem;
import mtm68.assem.LabelAssem;
import mtm68.assem.cfg.Graph.Node;
import mtm68.assem.operand.Loc;
import mtm68.util.ArrayUtils;
import polyglot.util.InternalCompilerError;

public class AssemCFGBuilder<T> {
	
	private Graph<AssemData<T>> graph;
	private	Node prev;
	private Node curr;
	private boolean prevWasLabel = false;
	private String lastLabel;
	
	private Map<Loc, Node> locationMap;
	private Map<Loc, List<Node>> waitingJumps;
	
	public AssemCFGBuilder() {
		graph = new Graph<>();
		locationMap = new HashMap<>();
		waitingJumps = new HashMap<>();
	}
	
	public Graph<AssemData<T>> buildAssemCFG(List<Assem> assems, Supplier<T> flowDataConstructor) {
		for(Assem assem : assems) {
			
			if(isJump(assem)) {
				handleJump((JumpAssem)assem);
				continue;
			}
			
			if(isLabel(assem)) {
				handleLabel((LabelAssem)assem);
				continue;
			}

			AssemData<T> data = new AssemData<>(assem, flowDataConstructor.get());
			curr = graph.createNode(data, assem.toString());
			
			if(prevWasLabel) {
				Loc loc = new Loc(lastLabel);
				locationMap.put(loc, curr);
				resolveWaitingJumps(loc);
				prevWasLabel = false;
			} else if(prev != null){
				graph.addEdge(prev, curr);
			}
			
			prev = curr;
		}
		
		if(waitingJumps.size() != 0) throw new InternalCompilerError("Still have jumps that need resolving: " + waitingJumps);
		
		return graph;
	}
	
	private void handleJump(JumpAssem assem) {
		Loc jumpTo = assem.getLoc();
		
		Node jumpTarget = locationMap.get(jumpTo);

		if(jumpTarget != null) {
			graph.addEdge(curr, jumpTarget);
		} else {
			addToWaitingJumps(jumpTo, curr);
		}
	}
	
	private void addToWaitingJumps(Loc loc, Node node) {
		if(!waitingJumps.containsKey(loc)) {
			waitingJumps.put(loc, ArrayUtils.empty());
		}
		
		waitingJumps.get(loc).add(node);
	}
	
	private void resolveWaitingJumps(Loc loc) {
		Node jumpTo = locationMap.get(loc);
		
		if(jumpTo == null) throw new InternalCompilerError("Resolving jump nodes but found null for location");
		
		if(waitingJumps.containsKey(loc)) {
			waitingJumps.get(loc).forEach(n -> graph.addEdge(n, jumpTo));
			waitingJumps.remove(loc);
		}
	}

	private void handleLabel(LabelAssem assem) {
		lastLabel = assem.getName();
		prevWasLabel = true;
	}
	
	private boolean isJump(Assem assem) {
		return assem instanceof JumpAssem;
	}

	private boolean isLabel(Assem assem) {
		return assem instanceof LabelAssem;
	}

	public static class AssemData<T> {
		
		private Assem assem;
		private T flowData;
		
		public AssemData(Assem assem, T flowData) {
			this.assem = assem;
			this.flowData = flowData;
		}
		
		public Assem getAssem() {
			return assem;
		}
		
		public T getFlowData() {
			return flowData;
		}
		
		public void setFlowData(T flowData) {
			this.flowData = flowData;
		}
	}
}
