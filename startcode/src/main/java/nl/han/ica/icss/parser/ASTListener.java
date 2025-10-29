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

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
		currentContainer.push(ast.root);

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
		Stylerule rule = (Stylerule) currentContainer.pop();
		currentContainer.peek().addChild(rule);
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
			throw new IllegalArgumentException("Onbekende literal: " + t);
		}
		currentContainer.push(lit);
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		if (!(currentContainer.peek() instanceof Expression)) return;
		Expression value = (Expression) currentContainer.pop();

		if (!(currentContainer.peek() instanceof Declaration)) return;
		Declaration decl = (Declaration) currentContainer.pop();

		decl.expression = value;
		currentContainer.peek().addChild(decl);
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx) {
		// Voorbeeld: MyVar
		VariableReference ref = new VariableReference(ctx.getText());
		currentContainer.push(ref); // zodat declaration/assignment hem kan oppakken
	}
	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		Expression value = null;
		if (currentContainer.peek() instanceof Expression) {
			value = (Expression) currentContainer.pop();
		}

		VariableAssignment assign = new VariableAssignment();
		assign.name = new VariableReference(ctx.CAPITAL_IDENT().getText());
		assign.expression = value;

		currentContainer.peek().addChild(assign);
	}
}