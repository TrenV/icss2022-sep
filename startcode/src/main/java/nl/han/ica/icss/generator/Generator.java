package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.*;

public class Generator {

	public String generate(AST ast) {
		StringBuilder css = new StringBuilder();

		for (ASTNode node : ast.root.getChildren()) {
			if (!(node instanceof Stylerule)) continue;

			Stylerule rule = (Stylerule) node;

			for (int i = 0; i < rule.selectors.size(); i++) {
				if (i > 0) css.append(", ");
				css.append(formatSelector(rule.selectors.get(i)));
			}
			css.append(" {\n");

			for (ASTNode child : rule.body) {
				if (!(child instanceof Declaration)) continue;

				Declaration d = (Declaration) child;
				String value = formatLiteral(d.expression);
				if (value == null) continue; // defensief; zou niet moeten gebeuren op lvl 3

				css.append("  ")
						.append(d.property.name)
						.append(": ")
						.append(value)
						.append(";\n");
			}

			css.append("}\n\n");
		}

		return css.toString();
	}


	private String formatSelector(Selector s) {
		if (s instanceof IdSelector)    return "#" + ((IdSelector) s).id;
		if (s instanceof ClassSelector) return "." + ((ClassSelector) s).cls;
		return ((TagSelector) s).tag;
	}

	private String formatLiteral(Expression expr) {
		if (!(expr instanceof Literal)) return null;

		Literal lit = (Literal) expr;
		if (lit instanceof ColorLiteral)      return ((ColorLiteral) lit).value;
		if (lit instanceof PixelLiteral)      return ((PixelLiteral) lit).value + "px";
		if (lit instanceof PercentageLiteral) return ((PercentageLiteral) lit).value + "%";
		if (lit instanceof ScalarLiteral)     return Integer.toString(((ScalarLiteral) lit).value);
		if (lit instanceof BoolLiteral)       return ((BoolLiteral) lit).value ? "true" : "false";

		return null;
	}
}