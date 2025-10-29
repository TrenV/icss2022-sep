package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.VariableReference;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	// Extra stack speciaal voor expressies (zoals literals, var refs, berekeningen)
	private IHANStack<Expression> exprStack;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
		currentContainer.push(ast.root);
		exprStack = new HANStack<>();
	}

	public AST getAST() {
		return ast;
	}

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		currentContainer.push(new Stylerule());
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		ASTNode node = currentContainer.pop();

		if (node instanceof Stylerule) {
			Stylerule rule = (Stylerule) node;

			ASTNode parent = currentContainer.peek();
			parent.addChild(rule);
		} else {
			System.err.println("Verwachtte Stylerule maar kreeg: " + node.getClass().getSimpleName());
		}
	}

	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		String raw = ctx.getText();
		Stylerule cur = (Stylerule) currentContainer.peek();
		if (raw.startsWith("#"))       cur.selectors.add(new IdSelector(raw.substring(1)));
		else if (raw.startsWith("."))  cur.selectors.add(new ClassSelector(raw.substring(1)));
		else                           cur.selectors.add(new TagSelector(raw));
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		currentContainer.push(new Declaration());
	}

	@Override
	public void exitProperty(ICSSParser.PropertyContext ctx) {
		Declaration decl = (Declaration) currentContainer.peek();
		decl.property = new PropertyName(ctx.getText().toLowerCase());
	}

	@Override
	public void exitLiteral(ICSSParser.LiteralContext ctx) {
		String t = ctx.getText();
		Literal lit;
		if (t.startsWith("#")) {
			lit = new ColorLiteral(t);
		} else if (t.endsWith("px")) {
			lit = new PixelLiteral(Integer.parseInt(t.substring(0, t.length()-2)));
		} else if (t.endsWith("%")) {
			lit = new PercentageLiteral(Integer.parseInt(t.substring(0, t.length()-1)));
		} else if (t.equalsIgnoreCase("true")) {
			lit = new BoolLiteral(true);
		} else if (t.equalsIgnoreCase("false")) {
			lit = new BoolLiteral(false);
		} else {
			lit = new ScalarLiteral(Integer.parseInt(t));
		}
		exprStack.push(lit);
	}
	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		if (exprStack.isEmpty()) return;
		Expression value = exprStack.pop();
		if (!(currentContainer.peek() instanceof Declaration)) return;
		Declaration decl = (Declaration) currentContainer.pop();

		decl.expression = value;
		currentContainer.peek().addChild(decl);
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx) {
		VariableReference ref = new VariableReference(ctx.getText());
		exprStack.push(ref);
	}

	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		Expression value = exprStack.isEmpty() ? null : exprStack.pop();		if (!exprStack.isEmpty()) {
			value = exprStack.pop();
		}

		VariableAssignment assign = new VariableAssignment();
		assign.name = new VariableReference(ctx.CAPITAL_IDENT().getText());
		assign.expression = value;

		currentContainer.peek().addChild(assign);
	}

	@Override
	public void exitAddExpr(ICSSParser.AddExprContext ctx) {
		if (exprStack.size() < 2) return;
		Expression right = exprStack.pop();
		Expression left = exprStack.pop();
		AddOperation op = new AddOperation();
		op.lhs = left;
		op.rhs = right;
		exprStack.push(op);
	}

	@Override
	public void exitSubExpr(ICSSParser.SubExprContext ctx) {
		if (exprStack.size() < 2) return;
		Expression right = exprStack.pop();
		Expression left = exprStack.pop();
		SubtractOperation op = new SubtractOperation();
		op.lhs = left;
		op.rhs = right;
		exprStack.push(op);
	}

	@Override
	public void exitMulExpr(ICSSParser.MulExprContext ctx) {
		if (exprStack.size() < 2) return;
		Expression right = exprStack.pop();
		Expression left = exprStack.pop();
		MultiplyOperation op = new MultiplyOperation();
		op.lhs = left;
		op.rhs = right;
		exprStack.push(op);
	}
}