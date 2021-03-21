package mtm68.types;

import static mtm68.ast.types.Types.*;
import static mtm68.util.ArrayUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import java_cup.runtime.ComplexSymbolFactory.Location;
import mtm68.ast.nodes.BoolLiteral;
import mtm68.ast.nodes.Expr;
import mtm68.ast.nodes.FExpr;
import mtm68.ast.nodes.IntLiteral;
import mtm68.ast.nodes.Node;
import mtm68.ast.nodes.stmts.Block;
import mtm68.ast.nodes.stmts.ExtendedDecl;
import mtm68.ast.nodes.stmts.FunctionCall;
import mtm68.ast.nodes.stmts.If;
import mtm68.ast.nodes.stmts.Return;
import mtm68.ast.nodes.stmts.SimpleDecl;
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
	// Block
	//-------------------------------------------------------------------------------- 

	@Test
	void emptyBlockIsUnit() {
		Block block = emptyBlock();
		Block newBlock = doTypeCheck(block);
		
		assertEquals(Result.UNIT, newBlock.getResult());
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
		stmt = doTypeCheck(stmt);
		
		assertEquals(Result.UNIT, stmt.getResult());
	}

	@Test
	void procedureCallUnboundFunction() {
		TypingContext context = new TypingContext();
		context.addFuncDecl("f", empty(), empty());

		FunctionCall stmt = new FunctionCall(new FExpr("g", empty()));
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
		context.addFuncDecl("f", empty(), singleton(INT));

		FExpr exp = new FExpr("f", empty());
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

	private <N extends Node> N doTypeCheck(TypingContext context, N node) {
		TypeChecker tc = new TypeChecker(context);
		addLocs(node);
		node = node.accept(tc);
		
		if(tc.hasError()) {
			assertTrue(false, "Expected no errors but got " + tc.getFirstError().getFileErrorMessage());
		}
		return node;
	}

	private <N extends Node> N doTypeCheck(N node) {
		TypeChecker tc = new TypeChecker();
		addLocs(node);
		node = node.accept(tc);
		assertFalse(tc.hasError(), "Expected no errors but got some");
		return node;
	}
	
	private <N extends Node> void assertTypeCheckError(TypingContext context, N node) {
		TypeChecker tc = new TypeChecker(context);
		addLocs(node);
		node.accept(tc);
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

	private BoolLiteral boolLit(boolean value) {
		return new BoolLiteral(value);
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