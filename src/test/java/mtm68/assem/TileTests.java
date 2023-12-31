package mtm68.assem;

import static mtm68.ir.IRTestUtils.*;
import static mtm68.util.ArrayUtils.*;
import static mtm68.util.TestUtils.*;

import org.junit.jupiter.api.Test;

import edu.cornell.cs.cs4120.ir.IRBinOp;
import edu.cornell.cs.cs4120.ir.IRBinOp.OpType;
import edu.cornell.cs.cs4120.ir.IRCJump;
import edu.cornell.cs.cs4120.ir.IRCallStmt;
import edu.cornell.cs.cs4120.ir.IRFuncDefn;
import edu.cornell.cs.cs4120.ir.IRMove;
import edu.cornell.cs.cs4120.ir.IRName;
import edu.cornell.cs.cs4120.ir.IRNode;
import edu.cornell.cs.cs4120.ir.IRNodeFactory_c;
import edu.cornell.cs.cs4120.ir.IRReturn;
import edu.cornell.cs.cs4120.ir.IRSeq;
import edu.cornell.cs.cs4120.ir.visit.Tiler;
import mtm68.util.Constants;

public class TileTests {
	
	private static final long LARGE_INT = 2 * (long)Integer.MAX_VALUE;
	
	@Test
	void tileConst() {
		IRNode constant = constant(12L);
		tile(constant);
	}

	@Test
	void tileMemAdd() {
		IRNode constant = mem(op(OpType.ADD, temp("t"), constant(12L)));
		tile(constant);
	}

	@Test
	void tileInClassExampleLargeInt() {
		IRMove move = move(
				mem(op(OpType.ADD,
						op(OpType.MUL,
								mem(op(OpType.ADD, constant(12L), temp("t"))),
								constant(4L)
								),
						mem(op(OpType.ADD,
								temp("t"),
								constant(LARGE_INT)))
						)),
				constant(7L)
			);
		tile(move);
	}

	@Test
	void tileInClassExampleSmallInt() {
		IRMove move = move(
				mem(op(OpType.ADD,
						op(OpType.MUL,
								mem(op(OpType.ADD, constant(12L), temp("t"))),
								constant(4L)
								),
						mem(op(OpType.ADD,
								temp("t"),
								constant(8L)))
						)),
				constant(7L)
			);
		tile(move);
	}

	@Test
	void tileAdd() {
		IRNode plus = op(OpType.ADD, temp("t1"), temp("t2"));
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(plus));
	}

	@Test
	void tileAddConstant() {
		IRNode plus = op(OpType.ADD, constant(12L), temp("t1"));
		tile(plus);
	}

	@Test
	void tileAddLargeConstant() {
		IRNode plus = op(OpType.ADD, temp("t1"), constant(LARGE_INT));
		tile(plus);
	}

	@Test
	void tileAddTwoConstants() {
		IRNode plus = op(OpType.ADD, constant(LARGE_INT), constant(2L));
		tile(plus);
	}

	@Test
	void tileSub() {
		IRNode sub = op(OpType.SUB, temp("t1"), constant(2L));
		tile(sub);
	}

	@Test
	void tileMul() {
		IRNode mul = op(OpType.MUL, temp("t1"), constant(2L));
		tile(mul);
	}
	
	@Test
	void tileBetterMem() {
		tile(move(temp("t"), mem(constant(-3L))));
		tile(move(temp("t"), mem(temp("t1"))));
		tile(move(temp("t"), mem(op(OpType.MUL, temp("t1"), constant(4L)))));
		tile(move(temp("t"), mem(op(OpType.MUL, temp("t1"), constant(5L)))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), constant(LARGE_INT)))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), constant(3L)))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), constant(-3L)))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), temp("t2")))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), op(OpType.ADD, temp("t2"), constant(-10L))))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), op(OpType.MUL, temp("t2"), constant(2L))))));
		tile(move(temp("t"), mem(op(OpType.ADD, temp("t1"), op(OpType.ADD, constant(-5L), op(OpType.MUL, temp("t2"), constant(2L)))))));
	}

	@Test
	void tileCmp() {
		IRSeq seq = new IRSeq(
				move(temp("res"), op(OpType.GEQ, temp("t1"), temp("t2"))),
				move(temp("res"), op(OpType.GT, temp("t1"), temp("t2"))),
				move(temp("res"), op(OpType.LEQ, temp("t1"), temp("t2"))),
				move(temp("res"), op(OpType.LT, temp("t1"), temp("t2"))),
				move(temp("res"), op(OpType.EQ, temp("t1"), temp("t2"))),
				move(temp("res"), op(OpType.NEQ, temp("t1"), temp("t2")))
				);
		tile(seq);
	}
	
	@Test
	void tileMod() {
		IRBinOp mod = new IRBinOp(OpType.MOD, temp("t1"), temp("t2"));
		tile(mod);
	}

	@Test
	void tileDiv() {
		IRBinOp div = new IRBinOp(OpType.DIV, temp("t1"), temp("t2"));
		tile(div);
	}

	@Test
	void tileHmul() {
		IRBinOp hmul = new IRBinOp(OpType.HMUL, temp("t1"), temp("t2"));
		tile(hmul);
	}
	
	@Test
	void tileJump() {
		IRNode jump = jump("f");
		JumpAssem tiled = assertInstanceOfAndReturn(JumpAssem.class, tile(jump));
	}
	
	@Test
	void tileMem() {
		IRNode mem = mem(temp("t1"));
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(mem));
		
	}
	
	@Test
	void tileMoveRegToReg() {
		IRNode move = move("t1", "t2");
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(move));
		
	}
	
	@Test
	void tileMoveAddMem() {
		// (MOVE (TEMP _t2) (MEM (ADD (TEMP _t8) (CONST -8))))
		IRMove move = move(
				temp("t2"), 
				mem(op(OpType.ADD, temp("t8"), constant(-8L))));
		tile(move);
	}
	
	@Test
	void tileMoveMemToMem() {
		// (MOVE (MEM (TEMP _t6)) (MEM (ADD (TEMP _t8) (MUL (TEMP _t7) (CONST 8)))))
		IRMove move = move(
				mem(temp("t6")),
				mem(op(OpType.ADD, temp("t8"),
						op(OpType.MUL, temp("t7"), constant(8L))))
				);
		tile(move);
	}
	
	@Test
	void tileMoveMemToReg() {
		IRNode move = move("t1", mem(temp("t2")));
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(move));
		
	}
	
	@Test
	void tileMoveRegToMem() {
		IRNode move = move(mem(temp("t2")), temp("t1"));
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(move));
		
	}
	
	@Test
	void tileMoveRegTtoMem2() {
		IRNode move = move(mem(op(OpType.ADD, temp("t1"), temp("t2"))), op(OpType.ADD, temp("t3"), temp("t4")));
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(move));
	}
	
	@Test
	void tileMoveConstIntoMem() {
		IRMove move = move(mem(temp("t")), constant(1L));
		tile(move);
	}

	@Test
	void tileSeq() {
		IRSeq seq = new IRSeq(elems(
					move("t1", "t2"),
					move(mem(temp("t3")), op(OpType.ADD, temp("t4"), temp("t5")))
				));
		
		SeqAssem tiled = assertInstanceOfAndReturn(SeqAssem.class, tile(seq));
	}
	
	@Test
	void tileCallExtraArgsAndRets() {
		IRCallStmt call = new IRCallStmt(new IRName("f"), 4, 
				temp("t1"),
				temp("t2"),
				temp("t3"),
				op(OpType.ADD, temp("t4"), temp("t4")),
				temp("t5"),
				temp("t6"),
				temp("t7"),
				temp("t8")
				);
		
		IRMove movRet2 = new IRMove(temp("t9"), temp("_RET2"));
		IRMove movRet3 = new IRMove(temp("t10"), temp("_RET3"));
		
		IRSeq seq = new IRSeq(call, movRet2, movRet3, ret());
		
		tile(seq);
	}
	
	@Test
	void tileCallExtraArgs() {
		IRCallStmt call = new IRCallStmt(new IRName("f"), 0, 
				temp("t1"),
				temp("t2"),
				temp("t3"),
				op(OpType.ADD, temp("t4"), temp("t4")),
				temp("t5"),
				temp("t6"),
				temp("t7"),
				temp("t8")
				);
		
		IRMove movRet2 = new IRMove(temp("t9"), temp("_RET0"));
		IRMove movRet3 = new IRMove(temp("t10"), temp("_RET1"));
		
		IRSeq seq = new IRSeq(call, movRet2, movRet3, ret());
		
		tile(seq);
	}

	@Test
	void tileCall() {
		IRCallStmt call = new IRCallStmt(new IRName("f"), 0, 
				temp("t1"),
				temp("t2"),
				temp("t3"),
				op(OpType.ADD, temp("t4"), temp("t4")),
				temp("t5"),
				temp("t6")
				);
		
		IRMove movRet2 = new IRMove(temp("t9"), temp("_RET0"));
		IRMove movRet3 = new IRMove(temp("t10"), temp("_RET1"));
		
		IRSeq seq = new IRSeq(call, movRet2, movRet3, ret());
		
		tile(seq);
	}

	@Test
	void tileCJump() {
		IRCJump cjump = cjump(op(OpType.ADD, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}
	
	@Test
	void tileCJumpLt() {
		IRCJump cjump = cjump(op(OpType.LT, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}
	
	@Test
	void tileCJumpLeq() {
		IRCJump cjump = cjump(op(OpType.LEQ, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}
	
	@Test
	void tileCJumpNeq() {
		IRCJump cjump = cjump(op(OpType.NEQ, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}
	
	@Test
	void tileCJumpGt() {
		IRCJump cjump = cjump(op(OpType.GT, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}
	
	@Test
	void tileCJumpGeq() {
		IRCJump cjump = cjump(op(OpType.GEQ, temp("t1"), temp("t2")), "true", null);  
		tile(cjump);
	}

	@Test
	void tileReturn() {
		IRReturn ret = new IRReturn(temp("t1"), temp("t2"), temp("t3"), temp("t4"));
		tile(ret, 10);
	}

	@Test
	void tileMoveArg() {
		IRSeq seq = new IRSeq(
				move("t1", "_ARG0"),
				move("t2", "_ARG1"),
				move("t3", "_ARG2"),
				move("t4", "_ARG3"),
				move("t5", "_ARG4"),
				move("t6", "_ARG5"),
				move("t7", "_ARG6"),
				move("t8", "_ARG7"),
				move("t9", "_ARG8")
				);
		tile(seq);
	}
	
	@Test
	void tileFuncDefn() {
		IRSeq body = new IRSeq(
				move("t1", "t2"),
				move(mem(temp("t3")), op(OpType.ADD, temp("t4"),temp("t5")))
				);
		IRFuncDefn func = new IRFuncDefn("f", body, 0);
		
		FuncDefnAssem tiled = assertInstanceOfAndReturn(FuncDefnAssem.class, tile(func));
	}
	
	@Test
	void tileBinOp() {
		IRNode node = op(OpType.SUB, temp("t4"), temp("t5"));
		tile(node);
		
		node = op(OpType.SUB, temp("t4"), constant(1L));
		tile(node);
		
		node = op(OpType.SUB, temp("t4"), mem(op(OpType.ADD, temp("t1"), temp("t2"))));
		tile(node);
	}

	private Assem tile(IRNode node, int numArgs) {
		System.out.println("Before\n=========\n" + node);
		Tiler tiler = new Tiler(new IRNodeFactory_c());
		tiler.setRetSpaceOff(Constants.WORD_SIZE * (Math.max(numArgs - 6, 0) + 1));

		IRNode result = tiler.visit(node);
		System.out.println("After\n=========\n" + result.getAssem());
		System.out.println();

		return result.getAssem();
	}

	private Assem tile(IRNode node) {
		return tile(node, 0);
	}
}