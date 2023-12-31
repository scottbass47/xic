package mtm68.assem.visit;

import static mtm68.assem.operand.RealReg.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mtm68.assem.Assem;
import mtm68.assem.CompUnitAssem;
import mtm68.assem.FuncDefnAssem;
import mtm68.assem.MoveAssem;
import mtm68.assem.RegisterAllocator;
import mtm68.assem.ReplaceableReg;
import mtm68.assem.ReplaceableReg.RegType;
import mtm68.assem.SeqAssem;
import mtm68.assem.operand.Mem;
import mtm68.assem.operand.RealReg;
import mtm68.util.ArrayUtils;

public class TrivialRegisterAllocator implements RegisterAllocator {
	
	private static final List<RealReg> SHUTTLE_REGS = Arrays.asList(R10, R11, R12);
	
	/**
	 * Does register allocation for each function in the CompUnitAssem. Each
	 * FuncDefAssem can be a mix of abstract or real registers. The list returned
	 * contains the assembly for all the functions where all Regs are RealRegs.
	 */
	@Override
	public CompUnitAssem allocateRegisters(CompUnitAssem assem) {
		List<FuncDefnAssem> newFuncs = assem.getFunctions().stream()
			.map(this::allocateForFunc)
			.collect(Collectors.toList());
		
		// flatten all seqs
		return new CompUnitAssem(assem.getName(), newFuncs);
	}

	private FuncDefnAssem allocateForFunc(FuncDefnAssem func) {
		List<Assem> insts = func.getBodyAssem().getAssems();
		Map<String, Mem> regsToLoc = assignAbstrRegsToStackLocations(insts);
		List<Assem> newBody = assignAbstrRegsToRealRegs(insts, regsToLoc);

		FuncDefnAssem newFunc = func.copy(); 
		newFunc.setBodyAssem(new SeqAssem(newBody));
		newFunc.setNumSpilledTemps(regsToLoc.size());
		return newFunc;
	}

	private Map<String, Mem> assignAbstrRegsToStackLocations(List<Assem> insts) {
		// TODO: consider sorting by temp name
		Set<String> regNames = getReplaceableRegNames(insts);
		return getRegStackLocs(regNames);
	}
	
	private Set<String> getReplaceableRegNames(List<Assem> insts) {
		return insts.stream()
					.map(Assem::getReplaceableRegs)
					.flatMap(List::stream)
					.filter(ReplaceableReg::isAbstract)
					.map(ReplaceableReg::getName)
					.collect(Collectors.toSet());
	}
	
	private Map<String, Mem> getRegStackLocs(Set<String> regNames) {
		Map<String, Mem> regToStackLocs = new HashMap<>();
		int size = 0;
		
		// stack location = [rbp - 8 * l]
		for(String regName : regNames) {
			Mem mem = new Mem(RealReg.RBP, (size+1) * - 8);
			regToStackLocs.put(regName, mem);
			size++;
		}
		
		return regToStackLocs;
	}

	private List<Assem> assignAbstrRegsToRealRegs(List<Assem> insts, Map<String, Mem> regsToLoc) {
		List<Assem> realAssem = ArrayUtils.empty();
		for(Assem inst : insts) {
			Assem newInst = assignAbstrRegsToRealRegs(inst, regsToLoc);
			realAssem.add(newInst);
		}
		return realAssem;
	}
	
	/**
	 * Returns the instruction(s) that moves from the stack location into a
	 * register, does the instruction using the real register, and moves from the
	 * register back to the stack location (if the register contents were mutated).
	 */
	private Assem assignAbstrRegsToRealRegs(Assem inst, Map<String, Mem> regsToLoc) {
		Assem newInst = inst.copy();

		Map<String, RealReg> replaceToRealMap = new LinkedHashMap<>();
		List<ReplaceableReg> replaceableRegs = newInst.getReplaceableRegs().stream()
				.filter(ReplaceableReg::isAbstract)
				.collect(Collectors.toList());
		
		Map<Boolean, List<ReplaceableReg>> partitioned = replaceableRegs.stream()
			.collect(Collectors.partitioningBy(r -> r.getRegType() == RegType.WRITE));
		
		List<ReplaceableReg> destRegs = partitioned.get(true);
		List<ReplaceableReg> srcRegs = partitioned.get(false);
		
		List<Assem> assems = ArrayUtils.empty();
		Iterator<RealReg> shuttleIterator = SHUTTLE_REGS.iterator();

		doShuttling(srcRegs, RegType.READ, replaceToRealMap, shuttleIterator, assems, regsToLoc);
		assems.add(newInst);
		doShuttling(destRegs, RegType.WRITE, replaceToRealMap, shuttleIterator, assems, regsToLoc);

		return new SeqAssem(assems);
		
	}
	
	private void doShuttling(List<ReplaceableReg> regs, RegType regType, Map<String, RealReg> replaceToRealMap,
			Iterator<RealReg> shuttleIterator, List<Assem> assems, Map<String, Mem> regsToLoc) {
		Set<String> alreadyShuttled = new HashSet<>();
		
		for(ReplaceableReg reg : regs) {
			String name = reg.getName();
			
			if(alreadyShuttled.contains(name)) {
				reg.replace(replaceToRealMap.get(name));
				continue;
			}
			
			RealReg shuttle = null;
			if(replaceToRealMap.containsKey(name)) {
				shuttle = replaceToRealMap.get(name);
			} else {
				shuttle = shuttleIterator.next();
				replaceToRealMap.put(name, shuttle);
			}

			reg.replace(shuttle);
			alreadyShuttled.add(name);
			
			Mem stackOffset = regsToLoc.get(name);
			
			if(regType == RegType.READ) {
				assems.add(new MoveAssem(shuttle, stackOffset));
			} else {
				assems.add(new MoveAssem(stackOffset, shuttle));
			}
		}
	}
}
