package mtm68.assem.cfg;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mtm68.assem.Assem;
import mtm68.assem.CompUnitAssem;
import mtm68.assem.FuncDefnAssem;
import mtm68.assem.SeqAssem;
import mtm68.assem.operand.RealReg;
import mtm68.util.ArrayUtils;
import mtm68.util.SetUtils;
import static mtm68.assem.AssemTestUtils.*;

public class RegisterAllocTests {
	
	private static final Set<RealReg> COLORS = SetUtils.elems(
			RealReg.R8,
			RealReg.R9,
			RealReg.R10,
			RealReg.R11,
			RealReg.R12,
			RealReg.R13,
			RealReg.R14,
			RealReg.R15,
			RealReg.RAX,
			RealReg.RBX,
			RealReg.RCX,
			RealReg.RDX,
			RealReg.RDI
		);
	
	@Test
	void registerAlloc() {
		Set<RealReg> colors = SetUtils.elems(RealReg.R12);
		RegisterAllocation regAlloc = new RegisterAllocation(colors);
		
		List<Assem> assems = ArrayUtils.elems(
				mov(reg("t1"), reg("t2")),
				mov(reg("t2"), reg("t3")),
				mov(reg("t1"), reg("t4")),
				ret()
			);

		FuncDefnAssem func = new FuncDefnAssem("f", 0, new SeqAssem(assems));
		CompUnitAssem program = new CompUnitAssem("test", ArrayUtils.singleton(func));
		
		CompUnitAssem newProgram = regAlloc.doRegisterAllocation(program);
		printResults(assems, newProgram);
		
//		System.out.println("Color map: " + regAlloc.getColorMap());
	}
	
	private void printResults(List<Assem> original, CompUnitAssem coloredProgram) {
		System.out.println("Original\n=========");
		printAssems(original);
		System.out.println();

		System.out.println("Colored\n=========");
		printAssems(coloredProgram.getFunctions().get(0).getFlattenedAssem().getAssems());
		System.out.println();
	}
	
	private void printAssems(List<Assem> assems) {
		assems.forEach(System.out::println);
	}

}
