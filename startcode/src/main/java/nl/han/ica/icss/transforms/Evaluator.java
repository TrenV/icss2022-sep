package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        variableValues.insert(0, new HashMap<>());
        for (ASTNode node : ast.root.getChildren()) {
            if (node instanceof VariableAssignment) evalVarAssign((VariableAssignment) node);
            if (node instanceof Stylerule) evalStyleRule((Stylerule) node);
        }
    }

    private void evalStyleRule(Stylerule rule) {
        variableValues.insert(0, new HashMap<>());
        List<ASTNode> newBody = new ArrayList<>();

        for (ASTNode child : rule.body) {
            if (child instanceof VariableAssignment) evalVarAssign((VariableAssignment) child);
            else if (child instanceof Declaration) {
                Declaration d = (Declaration) child;
                Literal lit = evalExpr(d.expression);
                if (lit != null) d.expression = lit;
                newBody.add(d);
            } else if (child instanceof IfClause) {
                List<ASTNode> result = evalIfClause((IfClause) child);
                newBody.addAll(result);
            }
        }

        rule.body.clear();
        rule.body.addAll(newBody);
        variableValues.delete(0);
    }

    private List<ASTNode> evalIfClause(IfClause clause) {
        Literal cond = evalExpr(clause.conditionalExpression);
        boolean takeIf = cond instanceof BoolLiteral && ((BoolLiteral) cond).value;
        if (takeIf) return clause.body;
        return (clause.elseClause != null && clause.elseClause.body != null) ? clause.elseClause.body : new ArrayList<>();
    }

    private void evalVarAssign(VariableAssignment assignment) {
        Literal lit = evalExpr(assignment.expression);
        if (lit != null) variableValues.getFirst().put(assignment.name.name, lit);
    }

    private Literal evalExpr(Expression expr) {
        if (expr == null) return null;
        if (expr instanceof Literal) return (Literal) expr;

        if (expr instanceof VariableReference) {
            for (int i = 0; i < variableValues.getSize(); i++) {
                HashMap<String, Literal> scope = variableValues.get(i);
                if (scope.containsKey(((VariableReference) expr).name)) {
                    return scope.get(((VariableReference) expr).name);
                }
            }
            return null;
        }

        if (expr instanceof Operation) {
            Operation op = (Operation) expr;
            Literal l = evalExpr(op.lhs);
            Literal r = evalExpr(op.rhs);

            if (op instanceof AddOperation) return calcAdd(l, r);
            if (op instanceof SubtractOperation) return calcSub(l, r);
            if (op instanceof MultiplyOperation) return calcMul(l, r);
        }

        return null;
    }

    private Literal calcAdd(Literal a, Literal b) {
        if (a instanceof PixelLiteral && b instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) a).value + ((PixelLiteral) b).value);
        if (a instanceof PercentageLiteral && b instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) a).value + ((PercentageLiteral) b).value);
        if (a instanceof ScalarLiteral && b instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) a).value + ((ScalarLiteral) b).value);
        return null;
    }

    private Literal calcSub(Literal a, Literal b) {
        if (a instanceof PixelLiteral && b instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) a).value - ((PixelLiteral) b).value);
        if (a instanceof PercentageLiteral && b instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) a).value - ((PercentageLiteral) b).value);
        if (a instanceof ScalarLiteral && b instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) a).value - ((ScalarLiteral) b).value);
        return null;
    }

    private Literal calcMul(Literal a, Literal b) {
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