package mtm68.types;

import static mtm68.ast.types.Types.ARRAY;
import static mtm68.ast.types.Types.BOOL;
import static mtm68.ast.types.Types.INT;
import static mtm68.ast.types.Types.TVEC;
import static mtm68.ast.types.Types.addArrayDims;
import static mtm68.util.ArrayUtils.elems;
import static mtm68.util.ArrayUtils.empty;
import static mtm68.util.ArrayUtils.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import java_cup.runtime.ComplexSymbolFactory.Location;
import mtm68.ast.nodes.BoolLiteral;
import mtm68.ast.nodes.CharLiteral;
import mtm68.ast.nodes.Expr;
import mtm68.ast.nodes.FExpr;
import mtm68.ast.nodes.FunctionDecl;
import mtm68.ast.nodes.FunctionDefn;
import mtm68.ast.nodes.IntLiteral;
import mtm68.ast.nodes.Node;
import mtm68.ast.nodes.StringLiteral;
import mtm68.ast.nodes.Var;
import mtm68.ast.nodes.binary.Add;
import mtm68.ast.nodes.binary.BinExpr;
import mtm68.ast.nodes.binary.EqEq;
import mtm68.ast.nodes.stmts.Block;
import mtm68.ast.nodes.stmts.ExtendedDecl;
import mtm68.ast.nodes.stmts.FunctionCall;
import mtm68.ast.nodes.stmts.If;
import mtm68.ast.nodes.stmts.Return;
import mtm68.ast.nodes.stmts.SimpleDecl;
import mtm68.ast.nodes.stmts.SingleAssign;
import mtm68.ast.nodes.stmts.While;
import mtm68.ast.types.DeclType;
import mtm68.ast.types.Result;
import mtm68.ast.types.Type;
import mtm68.ast.types.Types;
import mtm68.ast.types.TypingContext;
import mtm68.util.ArrayUtils;
import mtm68.visit.TypeChecker;
import mtm68.visit.Visitor;

public class TypeCheckerTests {

	//-------------------------------------------------------------------------------- 
	// IntLiteral 
	//-------------------------------------------------------------------------------- 

	@Test
	void intLiteralIsInt() {
		CharLiteral literal = charLit('c');
		CharLiteral newLiteral = doTypeCheck(literal);
		
		assertEquals(INT, newLiteral.getType());
	}

	@Test
	void charIsIntLiteral() {
		StringLiteral literal = stringLit("hello");
		StringLiteral newLiteral = doTypeCheck(literal);
		
		assertEquals(ARRAY(INT), newLiteral.getType());
	}
	
	//-------------------------------------------------------------------------------- 
	// BoolLiteral
	//-------------------------------------------------------------------------------- 

	@Test
	void trueIsBoolLiteral() {
		BoolLiteral literal = boolLit(true);
		BoolLiteral newLiteral = doTypeCheck(literal);
		
		assertEquals(BOOL, newLiteral.getType());
	}

	@Test
	void falseIsBoolLiteral() {
		BoolLiteral literal = boolLit(false);
		BoolLiteral newLiteral = doTypeCheck(literal);
		
		assertEquals(BOOL, newLiteral.getType());
	}

	//-------------------------------------------------------------------------------- 
	// StringLiteral
	//-------------------------------------------------------------------------------- 

	@Test
	void stringIsIntArray() {
		StringLiteral literal = stringLit("hello");
		StringLiteral newLiteral = doTypeCheck(literal);
		
		assertEquals(ARRAY(INT), newLiteral.getType());
	}

	//-------------------------------------------------------------------------------- 
	// BinExpr (Add, And, Div, EqEq, GreaterThan, GreaterThanOrEqual,
	//			HighMult, LessThan, LessThanOrEqual, Mod, Mult,
	//			Or, Sub)
	//-------------------------------------------------------------------------------- 

	@Test
	void addHasIntLeftAndIntRight() {
		BinExpr add = new Add(intLit(0L), intLit(1L));
		BinExpr newAdd = doTypeCheck(add);
		
		assertEquals(INT, newAdd.getLeft().getType());
		assertEquals(INT, newAdd.getRight().getType());
		assertEquals(INT, newAdd.getType());
	}

	@Test
	void addFailsWhenNotIntLeftAndIntRight() {
		BinExpr expr = new Add(arbitraryCondition(),arbitraryCondition());
		assertTypeCheckError(null, expr);
	}

	@Test
	void addFailsWhenNotIntLeft() {
		BinExpr expr = new Add(arbitraryCondition(),intLit(0L));
		assertTypeCheckError(null, expr);
	}

	@Test
	void addFailsWhenNotIntRight() {
		BinExpr expr = new Add(arbitraryCondition(),intLit(0L));
		assertTypeCheckError(null, expr);
	}

	@Test
	void eqEqHasTypeBool() {
		BinExpr eqeq = new EqEq(intLit(0L), intLit(1L));
		BinExpr newEqEq = doTypeCheck(eqeq);
		
		assertEquals(INT, newEqEq.getLeft().getType());
		assertEquals(INT, newEqEq.getRight().getType());
		assertEquals(BOOL, newEqEq.getType());
	}
	
	//-------------------------------------------------------------------------------- 
	// Var
	//-------------------------------------------------------------------------------- 
	
	@Test
	void varFailsWhenNotInScope() {
		TypingContext context = new TypingContext();
		Var var = new Var("x");

		assertTypeCheckError(context, var);
	}

	@Test
	void varIsAssignedFromContext() {
		TypingContext context = new TypingContext();
		context.addIdBinding("x", BOOL);

		Var var = new Var("x");
		Var newVar = doTypeCheck(context, var);
		
		assertEquals(Types.BOOL, newVar.getType());
	}


	//-------------------------------------------------------------------------------- 
	// Block
	//-------------------------------------------------------------------------------- 

	@Test
	void emptyBlockIsUnit() {
		Block block = emptyBlock();
		Block newBlock = doTypeCheck(block);
		
		assertEquals(Result.UNIT, newBlock.getResult());
	}

	@Test
	void blockAllStatementsUnit() {
		Block block = new Block(elems(
				new SimpleDecl("x", INT),
				new SimpleDecl("y", INT),
				new SimpleDecl("z", INT)
				));
		Block newBlock = doTypeCheck(block);
		
		assertEquals(Result.UNIT, newBlock.getResult());
	}

	@Test
	void blockMatchesTypeOfLastStmt() {
		TypingContext context = setupRho(empty());
		Block block = new Block(elems(
				new SimpleDecl("x", INT),
				new SimpleDecl("y", INT),
				new SimpleDecl("z", INT)
				), new Return(empty()));
		Block newBlock = doTypeCheck(context, block);
		
		assertEquals(Result.VOID, newBlock.getResult());
	}

	@Test
	void blockCantHaveVoidInMiddle() {
		TypingContext context = setupRho(empty());
		Block block = new Block(elems(
				new SimpleDecl("x", INT),
				new Block(empty(), new Return(empty())),
				new SimpleDecl("z", INT)
				), new Return(empty()));

		assertTypeCheckError(context, block);
	}

	//-------------------------------------------------------------------------------- 
	// If
	//-------------------------------------------------------------------------------- 

	@Test
	void ifNoElseIsUnit() {
		If ifStmt = new If(arbitraryCondition(), emptyBlock());
		ifStmt = doTypeCheck(ifStmt);
		
		assertEquals(Result.UNIT, ifStmt.getResult());
	}

	@Test
	void ifNoElseIsUnitDespiteVoidInner() {
		TypingContext gamma = setupRho(empty());
		If ifStmt = new If(arbitraryCondition(), new Block(empty(), new Return(empty())));
		ifStmt = doTypeCheck(gamma, ifStmt);
		
		assertEquals(Result.UNIT, ifStmt.getResult());
	}

	@Test
	void ifRestoresScope() {
		TypingContext context = new TypingContext();
		context.addIdBinding("z", BOOL);

		If ifStmt = new If(arbitraryCondition(), new SimpleDecl("x", INT));
		ifStmt = doTypeCheck(context, ifStmt);
		
		assertEquals(Result.UNIT, ifStmt.getResult());
		assertFalse(context.isDefined("x"));
		assertTrue(context.isDefined("z"));
	}

	@Test
	void ifWithElseBothVoidIsVoid() {
		TypingContext context = setupRho(empty());

		// if cond { return } else { return }
		If ifStmt = new If(arbitraryCondition(), 
				new Block(empty(), new Return(empty())),
				new Block(empty(), new Return(empty()))
			);
		ifStmt = doTypeCheck(context, ifStmt);
		
		assertEquals(Result.VOID, ifStmt.getResult());
	}

	@Test
	void ifWithElseOneUnitIsUnit() {
		TypingContext context = setupRho(empty());

		// if cond { x : int } else { return }
		If ifStmt = new If(arbitraryCondition(), 
				new SimpleDecl("x", INT),
				new Block(empty(), new Return(empty()))
			);
		ifStmt = doTypeCheck(context, ifStmt);
		
		assertEquals(Result.UNIT, ifStmt.getResult());
	}

	@Test
	void ifWithElseBranchesDontShareContext() {
		TypingContext context = new TypingContext();

		If ifStmt = new If(arbitraryCondition(), 
				new SimpleDecl("x", INT),
				new SimpleDecl("x", INT)
			);
		ifStmt = doTypeCheck(context, ifStmt);
		
		assertEquals(Result.UNIT, ifStmt.getResult());
	}

	//-------------------------------------------------------------------------------- 
	// While
	//-------------------------------------------------------------------------------- 

	@Test
	void whileValidIsUnit() {
		While whileStmt = new While(arbitraryCondition(), emptyBlock());
		whileStmt = doTypeCheck(whileStmt);
		
		assertEquals(Result.UNIT, whileStmt.getResult());
	}

	@Test
	void whileRequiresBooleanCondition() {
		While whileStmt = new While(intLit(0L), emptyBlock());
		assertTypeCheckError(whileStmt);
	}

	@Test
	void whileDoesntLeakScope() {
		TypingContext context = new TypingContext();
		context.addIdBinding("x", INT);

		While whileStmt = new While(arbitraryCondition(), new SimpleDecl("y", INT));
		whileStmt = doTypeCheck(context, whileStmt);

		assertFalse(context.isDefined("y"));
		assertTrue(context.isDefined("x"));
	}

	//-------------------------------------------------------------------------------- 
	// Procedure Call
	//-------------------------------------------------------------------------------- 

	@Test
	void procedureCallValid() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), empty());

		FunctionCall stmt = new FunctionCall(new FExpr("f", empty()));
		stmt = doTypeCheck(context, stmt);
		
		assertEquals(Result.UNIT, stmt.getResult());
	}

	@Test
	void procedureCallUnboundFunction() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), empty());

		FunctionCall stmt = new FunctionCall(new FExpr("g", empty()));
		assertTypeCheckError(context, stmt);
	}

	@Test
	void procedureCallDoesntReturnUnit() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), singleton(INT));

		FunctionCall stmt = new FunctionCall(new FExpr("f", empty()));
		assertTypeCheckError(context, stmt);
	}

	@Test
	void procedureCallMismatchNumArgs() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", singleton(new SimpleDecl("x", INT)), empty());

		FunctionCall stmt = new FunctionCall(new FExpr("f", empty()));
		assertTypeCheckError(context, stmt);
	}

	//-------------------------------------------------------------------------------- 
	// Decl
	//-------------------------------------------------------------------------------- 

	@Test
	void declAddsToContext() {
		TypingContext context = new TypingContext();
		SimpleDecl decl = new SimpleDecl("x", INT);
		decl = doTypeCheck(context, decl);
		
		assertEquals(Result.UNIT, decl.getResult());
		assertTrue(context.isDefined("x"));
	}

	@Test
	void declAlreadyInScopeError() {
		TypingContext context = new TypingContext();
		context.addIdBinding("x", BOOL);

		SimpleDecl decl = new SimpleDecl("x", INT);
		assertTypeCheckError(context, decl);
	}

	@Test
	void extendedDeclAlreadyInScopeError() {
		TypingContext context = new TypingContext();
		context.addIdBinding("x", BOOL);

		ExtendedDecl decl = new ExtendedDecl("x", new DeclType(INT, 
				elems(intLit(3L)), 
				1));
		assertTypeCheckError(context, decl);
	}

	@Test
	void extendedDeclArrayValid() {
		TypingContext context = new TypingContext();
		ExtendedDecl decl = new ExtendedDecl("x", new DeclType(INT, 
				elems(intLit(3L)), 
				1));

		decl = doTypeCheck(context, decl);
		
		assertEquals(Result.UNIT, decl.getResult());
		assertEquals(addArrayDims(INT, 2), context.getIdType("x"));
	}

	@Test
	void extendedDeclArrayNonIntIndex() {
		TypingContext context = new TypingContext();
		ExtendedDecl decl = new ExtendedDecl("x", new DeclType(INT, 
				elems(boolLit(false)), 
				1));

		assertTypeCheckError(context, decl);
	}

	//-------------------------------------------------------------------------------- 
	// Assign
	//-------------------------------------------------------------------------------- 

	@Test
	void singleAssignValid() {
		TypingContext context = new TypingContext();
		context.addIdBinding("x", INT);
		
		SingleAssign assign = new SingleAssign(new Var("x"), intLit(0L));
		assign = doTypeCheck(context, assign);
		
		assertEquals(Result.UNIT, assign.getResult());
		assertTrue(context.isDefined("x"));
	}

	//-------------------------------------------------------------------------------- 
	// Return
	//-------------------------------------------------------------------------------- 
	
	@Test
	void returnAlwaysVoid() {
		TypingContext gamma = setupRho(empty());

		Return ret = new Return(empty());
		ret = doTypeCheck(gamma, ret);

		assertEquals(Result.VOID, ret.getResult());
	}

	@Test
	void returnExprMismatchInNumberError() {
		TypingContext gamma = setupRho(elems(BOOL));

		Return ret = new Return(empty());
		assertTypeCheckError(gamma, ret);
	}

	@Test
	void returnExprMismatchInTypeError() {
		TypingContext gamma = setupRho(elems(INT));

		Return ret = new Return(elems(arbitraryCondition()));
		assertTypeCheckError(gamma, ret);
	}

	@Test
	void returnExprTypesMatch() {
		TypingContext gamma = setupRho(elems(BOOL));

		Return ret = new Return(elems(arbitraryCondition()));
		ret = doTypeCheck(gamma, ret);
		
		assertEquals(Result.VOID, ret.getResult());
	}

	@Test
	void returnMultipleExprTypesMatch() {
		TypingContext gamma = setupRho(elems(BOOL, INT));

		Return ret = new Return(elems(arbitraryCondition(), intLit(0L)));
		ret = doTypeCheck(gamma, ret);
		
		assertEquals(Result.VOID, ret.getResult());
	}

	@Test
	void returnMultipleExprTypesMismatch() {
		TypingContext gamma = setupRho(elems(addArrayDims(BOOL, 1), INT));

		Return ret = new Return(elems(arbitraryCondition(), intLit(0L)));
		assertTypeCheckError(gamma, ret);
	}

	//-------------------------------------------------------------------------------- 
	// FExp
	//-------------------------------------------------------------------------------- 
	
	@Test
	void fexpValid() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", 
				elems(new SimpleDecl("x", INT), new SimpleDecl("y", BOOL)), 
				singleton(INT));

		FExpr exp = new FExpr("f", elems(intLit(0L), boolLit(true)));
		exp = doTypeCheck(context, exp);

		assertEquals(Types.INT, exp.getType());
	}

	@Test
	void fexpMultipleReturnArgs() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), elems(INT, BOOL));

		FExpr exp = new FExpr("f", empty());
		exp = doTypeCheck(context, exp);

		assertEquals(TVEC(INT, BOOL), exp.getType());
	}

	@Test
	void fexpNoReturnInvalid() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), empty());

		FExpr exp = new FExpr("f", empty());
		assertTypeCheckError(context, exp);
	}

	@Test
	void fexpMismatchNumberOfArgs() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", singleton(new SimpleDecl("x", INT)), singleton(BOOL));

		FExpr exp = new FExpr("f", elems(intLit(0L), boolLit(true)));
		assertTypeCheckError(context, exp);
	}

	@Test
	void fexpMismatchArgTypes() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", elems(new SimpleDecl("x", INT), new SimpleDecl("y", INT)), singleton(BOOL));

		FExpr exp = new FExpr("f", elems(intLit(0L), boolLit(true)));
		assertTypeCheckError(context, exp);
	}
	
	//-------------------------------------------------------------------------------- 
	// FunctionDefn
	//-------------------------------------------------------------------------------- 
	
	@Test
	void procAnyResult() {
		FunctionDecl fDecl = new FunctionDecl("proc", elems(new SimpleDecl("x", INT)), ArrayUtils.empty());
		Block voidBlock = new Block(elems(
				new SimpleDecl("y", INT),
				new Return(ArrayUtils.empty())
				));
		
		FunctionDefn fDefn = new FunctionDefn(fDecl, voidBlock);
		fDefn = doTypeCheck(fDefn);
		
		assertEquals(Result.VOID, fDefn.getBody().getResult());
		
		Block unitBlock = new Block(elems(
				new SimpleDecl("y", INT)
				));
		
		FunctionDefn fDefn2 = new FunctionDefn(fDecl, unitBlock);
		fDefn2 = doTypeCheck(fDefn2);
		
		assertEquals(Result.UNIT, fDefn2.getBody().getResult());
	}
	
	@Test
	void funcOnlyVoidResult() {
		FunctionDecl fDecl = new FunctionDecl("f", elems(new SimpleDecl("x", INT)), elems(Types.INT));
		Block voidBlock = new Block(elems(
				new SimpleDecl("y", INT),
				new Return(elems(intLit(1L)))
				));
		
		FunctionDefn fDefn = new FunctionDefn(fDecl, voidBlock);
		fDefn = doTypeCheck(fDefn);
		
		assertEquals(Result.VOID, fDefn.getBody().getResult());

		Block unitBlock = new Block(elems(
				new SimpleDecl("y", INT)
				));
		
		FunctionDefn fDefn2 = new FunctionDefn(fDecl, unitBlock);
		assertTypeCheckError(fDefn2);
	}
	
	@Test
	void funcArgsInBodyScope() {
		FunctionDecl fDecl = new FunctionDecl("f", elems(new SimpleDecl("x", INT)), elems(Types.INT));
		Block block = new Block(elems(
				new SimpleDecl("x", INT),
				new Return(elems(intLit(1L)))
				));
		
		FunctionDefn fDefn = new FunctionDefn(fDecl, block);
		assertTypeCheckError(fDefn);
		
		Block block2 = new Block(elems(
				new Return(elems(new Var("x")))
				));
		
		FunctionDefn fDefn2 = new FunctionDefn(fDecl, block2);
		fDefn2 = doTypeCheck(fDefn2);
		assertEquals(Result.VOID, fDefn2.getBody().getResult());
	}
	

	//-------------------------------------------------------------------------------- 
	// Helper Methods
	//-------------------------------------------------------------------------------- 

	private <N extends Node> N doTypeCheck(TypingContext context, N node) {
		TypeChecker tc = new TypeChecker(context);
		addLocs(node);
		node = tc.performTypeCheck(node);
		
		if(tc.hasError()) {
			assertTrue(false, "Expected no errors but got " + tc.getFirstError().getFileErrorMessage());
		}
		return node;
	}

	private <N extends Node> N doTypeCheck(N node) {
		return doTypeCheck(new TypingContext(), node);
	}
	
	private <N extends Node> void assertTypeCheckError(TypingContext context, N node) {
		TypeChecker tc = new TypeChecker(context);
		addLocs(node);
		tc.performTypeCheck(node);
		assertTrue(tc.hasError(), "Expected type check error but got none");
	}

	private <N extends Node> void assertTypeCheckError(N node) {
		assertTypeCheckError(new TypingContext(), node);
	}
	
	private TypingContext setupRho(List<Type> retTypes) {
		TypingContext context = new TypingContext();
		context.addFuncBindings(empty(), retTypes);
		return context;
	}
	
	private Expr arbitraryCondition() {
		return new BoolLiteral(true);
	}

	private IntLiteral intLit(Long value) {
		return new IntLiteral(value);
	}
	
	private CharLiteral charLit(Character value) {
		return new CharLiteral(value);
	}

	private BoolLiteral boolLit(boolean value) {
		return new BoolLiteral(value);
	}

	private StringLiteral stringLit(String value) {
		return new StringLiteral(value);
	}

	private Block emptyBlock() {
		return new Block(ArrayUtils.empty());
	}
	
	private void addLocs(Node n) {
		n.accept(new Visitor() {
			@Override
			public Node leave(Node n, Node old) {
				n.setStartLoc(new Location(0, 0));
				return n;
			}
		});
	}
}
