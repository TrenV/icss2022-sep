package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;

public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        variableTypes.insert(0, new HashMap<>()); // globale scope
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.getChildren()) {
            if (node instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Stylerule) {
                checkStylerule((Stylerule) node);
            }
        }
    }

    private void checkStylerule(Stylerule rule) {
        variableTypes.insert(0, new HashMap<>()); // nieuwe scope

        for (ASTNode child : rule.getChildren()) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            }
        }

        variableTypes.delete(0);
    }

    private void checkIfClause(IfClause clause) {
        ExpressionType condType = getExpressionType(clause.conditionalExpression);
        if (condType != ExpressionType.BOOL) {
            clause.conditionalExpression.setError("If-conditie moet boolean zijn.");
        }

        variableTypes.insert(0, new HashMap<>());
        for (ASTNode node : clause.body) {
            if (node instanceof Declaration) checkDeclaration((Declaration) node);
            if (node instanceof VariableAssignment) checkVariableAssignment((VariableAssignment) node);
        }
        variableTypes.delete(0);

        if (clause.elseClause != null && clause.elseClause.body != null) {
            variableTypes.insert(0, new HashMap<>());
            for (ASTNode node : clause.elseClause.body) {
                if (node instanceof Declaration) checkDeclaration((Declaration) node);
                if (node instanceof VariableAssignment) checkVariableAssignment((VariableAssignment) node);
            }
            variableTypes.delete(0);
        }
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType type = getExpressionType(assignment.expression);
        if (type == ExpressionType.UNDEFINED) {
            assignment.setError("Ongeldige waarde voor variabele " + assignment.name.name);
        } else {
            variableTypes.getFirst().put(assignment.name.name, type);
        }
    }

    private void checkDeclaration(Declaration declaration) {
        ExpressionType exprType = getExpressionType(declaration.expression);
        String property = declaration.property.name;

        if (exprType == ExpressionType.UNDEFINED) {
            declaration.setError("Ongeldige expressie in declaratie.");
            return;
        }

        if (property.equals("color") || property.equals("background-color")) {
            if (exprType != ExpressionType.COLOR) {
                declaration.setError("Property '" + property + "' verwacht een kleur.");
            }
        } else if (property.equals("width") || property.equals("height")) {
            if (exprType != ExpressionType.PIXEL && exprType != ExpressionType.PERCENTAGE) {
                declaration.setError("Property '" + property + "' verwacht px of %.");
            }
        }
    }

    private ExpressionType getExpressionType(Expression expr) {
        if (expr instanceof ColorLiteral) return ExpressionType.COLOR;
        if (expr instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (expr instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (expr instanceof ScalarLiteral) return ExpressionType.SCALAR;
        if (expr instanceof BoolLiteral) return ExpressionType.BOOL;

        if (expr instanceof VariableReference) {
            String name = ((VariableReference) expr).name;
            for (int i = 0; i < variableTypes.getSize(); i++) {
                HashMap<String, ExpressionType> scope = variableTypes.get(i);
                if (scope.containsKey(name)) return scope.get(name);
            }
            expr.setError("Onbekende variabele: " + name);
            return ExpressionType.UNDEFINED;
        }

        if (expr instanceof AddOperation) {
            ExpressionType lhs = getExpressionType(((AddOperation) expr).lhs);
            ExpressionType rhs = getExpressionType(((AddOperation) expr).rhs);
            return validateAddSub(expr, lhs, rhs);
        }
        if (expr instanceof SubtractOperation) {
            ExpressionType lhs = getExpressionType(((SubtractOperation) expr).lhs);
            ExpressionType rhs = getExpressionType(((SubtractOperation) expr).rhs);
            return validateAddSub(expr, lhs, rhs);
        }

        if (expr instanceof MultiplyOperation) {
            ExpressionType lhs = getExpressionType(((MultiplyOperation) expr).lhs);
            ExpressionType rhs = getExpressionType(((MultiplyOperation) expr).rhs);

            if (lhs == ExpressionType.SCALAR && rhs == ExpressionType.PIXEL) return ExpressionType.PIXEL;
            if (lhs == ExpressionType.PIXEL && rhs == ExpressionType.SCALAR) return ExpressionType.PIXEL;
            if (lhs == ExpressionType.SCALAR && rhs == ExpressionType.PERCENTAGE) return ExpressionType.PERCENTAGE;
            if (lhs == ExpressionType.PERCENTAGE && rhs == ExpressionType.SCALAR) return ExpressionType.PERCENTAGE;
            if (lhs == ExpressionType.SCALAR && rhs == ExpressionType.SCALAR) return ExpressionType.SCALAR;

            expr.setError("Ongeldige vermenigvuldiging tussen " + lhs + " en " + rhs);
            return ExpressionType.UNDEFINED;
        }

        return ExpressionType.UNDEFINED;
    }

    private ExpressionType validateAddSub(Expression expr, ExpressionType lhs, ExpressionType rhs) {
        if (lhs == rhs && lhs != ExpressionType.COLOR) return lhs;
        expr.setError("Ongeldige optelling/aftrekking tussen " + lhs + " en " + rhs);
        return ExpressionType.UNDEFINED;
    }
}