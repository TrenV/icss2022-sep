package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.*;

import java.util.HashMap;
import java.util.Map;

public class Generator {

	public String generate(AST ast) {
		StringBuilder css = new StringBuilder();
		Map<String, Literal> env = new HashMap<>();

		for (ASTNode node : ast.root.getChildren()) {
			// Variabele toewijzing bovenaan
			if (node instanceof VariableAssignment) {
				VariableAssignment va = (VariableAssignment) node;
				Literal val = evaluate(va.expression, env);
				if (val != null && va.name != null) {
					env.put(va.name.name, val);
				}
				continue;
			}

			// Alleen stylerules genereren
			if (!(node instanceof Stylerule)) continue;
			Stylerule rule = (Stylerule) node;

			// Selector(s)
			for (int i = 0; i < rule.selectors.size(); i++) {
				if (i > 0) css.append(", ");
				css.append(formatSelector(rule.selectors.get(i)));
			}
			css.append(" {\n");

			// Body met declaraties
			for (ASTNode child : rule.getChildren()) {
				if (!(child instanceof Declaration)) continue;
				Declaration d = (Declaration) child;
				Literal value = evaluate(d.expression, env);
				if (value == null) continue;

				css.append("  ")
						.append(d.property.name)
						.append(": ")
						.append(formatLiteral(value))
						.append(";\n");
			}

			css.append("}\n\n");
		}
		return css.toString();
	}

	private String formatSelector(Selector s) {
		if (s instanceof IdSelector)    return "#" + ((IdSelector) s).id;
		if (s instanceof ClassSelector) return "." + ((ClassSelector) s).cls;
		if (s instanceof TagSelector)   return ((TagSelector) s).tag;
		return "";
	}

	private String formatLiteral(Literal lit) {
		if (lit instanceof ColorLiteral)      return ((ColorLiteral) lit).value;
		if (lit instanceof PixelLiteral)      return ((PixelLiteral) lit).value + "px";
		if (lit instanceof PercentageLiteral) return ((PercentageLiteral) lit).value + "%";
		if (lit instanceof ScalarLiteral)     return Integer.toString(((ScalarLiteral) lit).value);
		if (lit instanceof BoolLiteral)       return ((BoolLiteral) lit).value ? "true" : "false";
		return "";
	}

	private Literal evaluate(Expression expr, Map<String, Literal> env) {
		if (expr == null) return null;

		// directe literal
		if (expr instanceof Literal) return (Literal) expr;

		// variabele lookup
		if (expr instanceof VariableReference) {
			return env.get(((VariableReference) expr).name);
		}

		// optellingen
		if (expr instanceof AddOperation) {
			Literal a = evaluate(((AddOperation) expr).lhs, env);
			Literal b = evaluate(((AddOperation) expr).rhs, env);
			return add(a, b);
		}

		// aftrekkingen
		if (expr instanceof SubtractOperation) {
			Literal a = evaluate(((SubtractOperation) expr).lhs, env);
			Literal b = evaluate(((SubtractOperation) expr).rhs, env);
			return sub(a, b);
		}

		// vermenigvuldigingen
		if (expr instanceof MultiplyOperation) {
			Literal a = evaluate(((MultiplyOperation) expr).lhs, env);
			Literal b = evaluate(((MultiplyOperation) expr).rhs, env);
			return mul(a, b);
		}

		return null;
	}

	private Literal add(Literal a, Literal b) {
		if (a == null || b == null) return null;
		if (a instanceof PixelLiteral && b instanceof PixelLiteral)
			return new PixelLiteral(((PixelLiteral) a).value + ((PixelLiteral) b).value);
		if (a instanceof PercentageLiteral && b instanceof PercentageLiteral)
			return new PercentageLiteral(((PercentageLiteral) a).value + ((PercentageLiteral) b).value);
		if (a instanceof ScalarLiteral && b instanceof ScalarLiteral)
			return new ScalarLiteral(((ScalarLiteral) a).value + ((ScalarLiteral) b).value);
		return null;
	}

	private Literal sub(Literal a, Literal b) {
		if (a == null || b == null) return null;
		if (a instanceof PixelLiteral && b instanceof PixelLiteral)
			return new PixelLiteral(((PixelLiteral) a).value - ((PixelLiteral) b).value);
		if (a instanceof PercentageLiteral && b instanceof PercentageLiteral)
			return new PercentageLiteral(((PercentageLiteral) a).value - ((PercentageLiteral) b).value);
		if (a instanceof ScalarLiteral && b instanceof ScalarLiteral)
			return new ScalarLiteral(((ScalarLiteral) a).value - ((ScalarLiteral) b).value);
		return null;
	}

	private Literal mul(Literal a, Literal b) {
		if (a == null || b == null) return null;

		if (a instanceof ScalarLiteral && b instanceof PixelLiteral)
			return new PixelLiteral(((ScalarLiteral) a).value * ((PixelLiteral) b).value);
		if (a instanceof PixelLiteral && b instanceof ScalarLiteral)
			return new PixelLiteral(((PixelLiteral) a).value * ((ScalarLiteral) b).value);

		if (a instanceof ScalarLiteral && b instanceof PercentageLiteral)
			return new PercentageLiteral(((ScalarLiteral) a).value * ((PercentageLiteral) b).value);
		if (a instanceof PercentageLiteral && b instanceof ScalarLiteral)
			return new PercentageLiteral(((PercentageLiteral) a).value * ((ScalarLiteral) b).value);

		if (a instanceof ScalarLiteral && b instanceof ScalarLiteral)
			return new ScalarLiteral(((ScalarLiteral) a).value * ((ScalarLiteral) b).value);

		return null;
	}
}