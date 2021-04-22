package mtm68.assem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mtm68.assem.JumpAssem.JumpType;
import mtm68.assem.op.AddAssem;
import mtm68.assem.op.LeaAssem;
import mtm68.assem.op.SubAssem;
import mtm68.assem.operand.AbstractReg;
import mtm68.assem.operand.Loc;
import mtm68.assem.operand.Mem;
import mtm68.assem.operand.RealReg;
import mtm68.assem.visit.TrivialRegisterAllocator;
import static mtm68.util.TestUtils.*;

public class TrivialRegisterAllocatorTests {
	
	@Test
	public void testNoAbstr() {
		List<Assem> insts = allocateSingleFunc(
				new CallAssem("g"),
				//new CmpAssem(),
				new JumpAssem(JumpType.JMP, loc("header")),
				new LabelAssem("lbl"),
				new MoveAssem(RealReg.RAX, RealReg.RBX),
				new PushAssem(RealReg.RAX),
				//new TestAssem(),
				new AddAssem(RealReg.RAX, RealReg.RBX),
				new LeaAssem(RealReg.RAX, RealReg.RBX),
				new SubAssem(RealReg.RAX, RealReg.RBX)
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	// -----------------------------------------------------------------
	// OneOpInst
	// -----------------------------------------------------------------
	@Test
	public void testOneOpAssemRealReg() {
		List<Assem> insts = allocateSingleFunc(
				new PushAssem(RealReg.RCX)
				);
		assertAllRealReg(insts);
		PushAssem p = assertInstanceOfAndReturn(PushAssem.class, insts.get(0));
		assertEquals(RealReg.RCX, p.getReg());
	}
	
	@Test
	public void testOneOpAssemAllRealReg() {
		List<Assem> insts = allocateSingleFunc(
				new PushAssem(RealReg.RCX),
				new PushAssem(RealReg.RCX),
				new PushAssem(RealReg.RBP)
				);
		assertAllRealReg(insts);
		PushAssem p0 = assertInstanceOfAndReturn(PushAssem.class, insts.get(0));
		assertEquals(RealReg.RCX, p0.getReg());
		PushAssem p1 = assertInstanceOfAndReturn(PushAssem.class, insts.get(1));
		assertEquals(RealReg.RCX, p1.getReg());
		PushAssem p2 = assertInstanceOfAndReturn(PushAssem.class, insts.get(2));
		assertEquals(RealReg.RBP, p2.getReg());
	}
	
	@Test
	public void testOneOpAssemAbstrReg() {
		List<Assem> insts = allocateSingleFunc(
				new PushAssem(abstrReg("t"))
				);

		assertAllRealReg(insts);
		MoveAssem m1 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(0));
		assertEquals(RealReg.R9, m1.getDest());
		assertInstanceOfAndReturn(Mem.class, m1.getSrc());
		PushAssem p = assertInstanceOfAndReturn(PushAssem.class, insts.get(1));
		assertEquals(RealReg.R9, p.getReg());
		MoveAssem m2 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(2));
		assertEquals(RealReg.R9, m2.getSrc());
		assertInstanceOfAndReturn(Mem.class, m2.getDest());

	}
	
	// -----------------------------------------------------------------
	// TwoOpInst
	// -----------------------------------------------------------------

	@Test
	public void testTwoOpAssemNoAbstr() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(RealReg.RAX, RealReg.RBX)
				);

		assertAllRealReg(insts);
		AddAssem a = assertInstanceOfAndReturn(AddAssem.class, insts.get(0));
		assertEquals(RealReg.RAX, a.getDest());	
		assertEquals(RealReg.RBX, a.getSrc());	
	}

	@Test
	public void testTwoOpAssemOneAbstr() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), RealReg.RBX)
				);
		assertAllRealReg(insts);
		
		MoveAssem m1 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(0));
		assertEquals(RealReg.R9, m1.getDest());
		assertInstanceOfAndReturn(Mem.class, m1.getSrc());

		AddAssem a = assertInstanceOfAndReturn(AddAssem.class, insts.get(1));
		assertEquals(RealReg.R9, a.getDest());
		assertEquals(RealReg.RBX, a.getSrc());
		
		MoveAssem m2 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(2));
		assertEquals(RealReg.R9, m2.getSrc());
		assertInstanceOfAndReturn(Mem.class, m2.getDest());
	}
	
	@Test
	public void testTwoOpAssemOneAbstr2() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(RealReg.RBX, abstrReg("t1"))
				);
		
		MoveAssem m1 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(0));
		assertEquals(RealReg.R9, m1.getDest());
		assertInstanceOfAndReturn(Mem.class, m1.getSrc());

		AddAssem a = assertInstanceOfAndReturn(AddAssem.class, insts.get(1));
		assertEquals(RealReg.RBX, a.getDest());
		assertEquals(RealReg.R9, a.getSrc());
		
		MoveAssem m2 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(2));
		assertEquals(RealReg.R9, m2.getSrc());
		assertInstanceOfAndReturn(Mem.class, m2.getDest());
		assertAllRealReg(insts);
	}	
	
	@Test
	public void testTwoOpAssemTwoAbstrDifferent() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t2"))
				);
		assertAllRealReg(insts);
		MoveAssem m1 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(0));
		assertEquals(RealReg.R9, m1.getDest());
		assertInstanceOfAndReturn(Mem.class, m1.getSrc());

		MoveAssem m2 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(1));
		assertEquals(RealReg.R10, m2.getDest());
		assertInstanceOfAndReturn(Mem.class, m2.getSrc());

		AddAssem a = assertInstanceOfAndReturn(AddAssem.class, insts.get(2));
		assertEquals(RealReg.R9, a.getDest());
		assertEquals(RealReg.R10, a.getSrc());
		
		MoveAssem m3 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(3));
		assertEquals(RealReg.R9, m3.getSrc());
		assertInstanceOfAndReturn(Mem.class, m3.getDest());

		MoveAssem m4 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(4));
		assertEquals(RealReg.R10, m4.getSrc());
		assertInstanceOfAndReturn(Mem.class, m4.getDest());
	}
	
	@Test
	public void testTwoOpAssemTwoAbstrSame() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t1"))
				);
		printInsts(insts);
		assertAllRealReg(insts);
		
		MoveAssem m1 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(0));
		assertEquals(RealReg.R9, m1.getDest());
		assertInstanceOfAndReturn(Mem.class, m1.getSrc());

		AddAssem a = assertInstanceOfAndReturn(AddAssem.class, insts.get(1));
		assertEquals(RealReg.R9, a.getDest());
		assertEquals(RealReg.R9, a.getSrc());
		
		MoveAssem m2 = assertInstanceOfAndReturn(MoveAssem.class, insts.get(2));
		assertEquals(RealReg.R9, m2.getSrc());
		assertInstanceOfAndReturn(Mem.class, m2.getDest());
		assertAllRealReg(insts);
	}

	@Test
	public void testTwoOpAssemOneAbstrMultipleInst() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), RealReg.RBX),
				new SubAssem(RealReg.RAX, abstrReg("t1"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	@Test
	public void testTwoOpAssemOneAbstrMultipleInst2() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t1")),
				new SubAssem(RealReg.RAX, abstrReg("t1"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	@Test
	public void testTwoOpAssemOneAbstrMultipleInst3() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t1")),
				new SubAssem(abstrReg("t1"), abstrReg("t1"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	@Test
	public void testTwoOpAssemTwoAbstrMultipleInst() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), RealReg.RBX),
				new SubAssem(RealReg.RAX, abstrReg("t2"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	@Test
	public void testTwoOpAssemTwoAbstrMultipleInst2() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t2")),
				new SubAssem(RealReg.RAX, abstrReg("t2"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
		
	@Test
	public void testTwoOpAssemTwoAbstrMultipleInst3() {
		List<Assem> insts = allocateSingleFunc(
				new AddAssem(abstrReg("t1"), abstrReg("t2")),
				new SubAssem(abstrReg("t2"), abstrReg("t1"))
				);
		assertAllRealReg(insts);
		printInsts(insts);
	}
	
	@Test
	public void testMultipleInstsSameAbstr() {
		
	}

	@Test
	public void testMultiFunc() {
		
	}
	
	private void assertAllRealReg(List<Assem> insts) {
		for(Assem inst : insts) {
			assertEquals(0, inst.getAbstractRegs().size());
		}
	}

	private AbstractReg abstrReg(String id) {
		return new AbstractReg(id);
	}

	private Loc loc(String name) {
		return new Loc(name);
	}

	private List<Assem> allocateSingleFunc(Assem...assems) {
		SeqAssem seq = new SeqAssem(assems);
		FuncDefnAssem func = new FuncDefnAssem("f", seq);
		
		List<FuncDefnAssem> funcs = new ArrayList<>();
		funcs.add(func);
		CompUnitAssem comp = new CompUnitAssem("test.xi", funcs);

		return allocate(comp);
	}

	private List<Assem> allocate(CompUnitAssem a) {
		TrivialRegisterAllocator allocator = new TrivialRegisterAllocator();
		List<Assem> result = allocator.allocate(a);
		return result;
	}

	private void printInsts(List<Assem> insts) {
		insts.stream().forEach(System.out::println);
	}
}
