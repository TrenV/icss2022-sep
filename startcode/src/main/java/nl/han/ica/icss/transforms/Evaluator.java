package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        variableValues.insert(0, new HashMap<>()); // globale scope
        evaluateStylesheet(ast.root);
    }

    private void evaluateStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.getChildren()) {
            if (node instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Stylerule) {
                evaluateStylerule((Stylerule) node);
            }
        }
    }

    private void evaluateStylerule(Stylerule rule) {
        variableValues.insert(0, new HashMap<>());
        ArrayList<ASTNode> newBody = new ArrayList<>();

        for (ASTNode child : rule.body) {
            if (child instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) child);

            } else if (child instanceof Declaration) {
                Declaration decl = (Declaration) child;
                Literal lit = evalToLiteral(decl.expression);
                if (lit != null) {
                    decl.expression = lit;
                    newBody.add(decl);
                }

            } else if (child instanceof IfClause) {
                ArrayList<ASTNode> chosenBody = evaluateIfClause((IfClause) child);
                for (ASTNode chosen : chosenBody) {
                    if (chosen instanceof VariableAssignment) {
                        evalVariableAssignment((VariableAssignment) chosen);
                    } else if (chosen instanceof Declaration) {
                        Declaration decl = (Declaration) chosen;
                        Literal lit = evalToLiteral(decl.expression);
                        if (lit != null) {
                            decl.expression = lit;
                            newBody.add(decl);
                        }
                    }
                }
            }
        }

        HashMap<String, Declaration> latest = new HashMap<>();
        for (ASTNode n : newBody) {
            if (n instanceof Declaration) {
                Declaration d = (Declaration) n;
                latest.put(d.property.name, d);
            }
        }

        rule.body.clear();
        for (ASTNode n : newBody) {
            if (n instanceof Declaration) {
                Declaration d = (Declaration) n;
                if (latest.get(d.property.name) == d) {
                    rule.addChild(d);
                }
            }
        }

        variableValues.delete(0);
    }

    private ArrayList<ASTNode> evaluateIfClause(IfClause clause) {
        ArrayList<ASTNode> result = new ArrayList<>();
        Literal condition = evalToLiteral(clause.conditionalExpression);
        boolean takeIf = (condition instanceof BoolLiteral) && ((BoolLiteral) condition).value;

        if (takeIf && clause.body != null) {
            result.addAll(clause.body);
        } else if (!takeIf && clause.elseClause != null && clause.elseClause.body != null) {
            result.addAll(clause.elseClause.body);
        }

        return result;
    }

    private void evalVariableAssignment(VariableAssignment a) {
        Literal lit = evalToLiteral(a.expression);
        if (lit != null) {
            variableValues.getFirst().put(a.name.name, lit);
        }
    }

    private Literal evalToLiteral(Expression e) {
        if (e == null) return null;

        if (e instanceof Literal) return (Literal) e;

        if (e instanceof VariableReference) {
            Literal val = lookup(((VariableReference) e).name);
            return val != null ? cloneLiteral(val) : null;
        }

        if (e instanceof AddOperation) {
            Literal l = evalToLiteral(((AddOperation) e).lhs);
            Literal r = evalToLiteral(((AddOperation) e).rhs);
            return add(l, r);
        }

        if (e instanceof SubtractOperation) {
            Literal l = evalToLiteral(((SubtractOperation) e).lhs);
            Literal r = evalToLiteral(((SubtractOperation) e).rhs);
            return sub(l, r);
        }

        if (e instanceof MultiplyOperation) {
            Literal l = evalToLiteral(((MultiplyOperation) e).lhs);
            Literal r = evalToLiteral(((MultiplyOperation) e).rhs);
            return mul(l, r);
        }

        return null;
    }

    private Literal add(Literal L, Literal R) {
        if (L == null || R == null) return null;
        if (L instanceof PixelLiteral && R instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) L).value + ((PixelLiteral) R).value);
        if (L instanceof PercentageLiteral && R instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) L).value + ((PercentageLiteral) R).value);
        if (L instanceof ScalarLiteral && R instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) L).value + ((ScalarLiteral) R).value);
        return null;
    }

    private Literal sub(Literal L, Literal R) {
        if (L == null || R == null) return null;
        if (L instanceof PixelLiteral && R instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) L).value - ((PixelLiteral) R).value);
        if (L instanceof PercentageLiteral && R instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) L).value - ((PercentageLiteral) R).value);
        if (L instanceof ScalarLiteral && R instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) L).value - ((ScalarLiteral) R).value);
        return null;
    }

    private Literal mul(Literal L, Literal R) {
        if (L == null || R == null) return null;

        if (L instanceof ScalarLiteral && R instanceof PixelLiteral)
            return new PixelLiteral(((ScalarLiteral) L).value * ((PixelLiteral) R).value);
        if (L instanceof PixelLiteral && R instanceof ScalarLiteral)
            return new PixelLiteral(((PixelLiteral) L).value * ((ScalarLiteral) R).value);

        if (L instanceof ScalarLiteral && R instanceof PercentageLiteral)
            return new PercentageLiteral(((ScalarLiteral) L).value * ((PercentageLiteral) R).value);
        if (L instanceof PercentageLiteral && R instanceof ScalarLiteral)
            return new PercentageLiteral(((PercentageLiteral) L).value * ((ScalarLiteral) R).value);

        if (L instanceof ScalarLiteral && R instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) L).value * ((ScalarLiteral) R).value);

        return null;
    }

    private Literal lookup(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    private Literal cloneLiteral(Literal lit) {
        if (lit instanceof PixelLiteral) return new PixelLiteral(((PixelLiteral) lit).value);
        if (lit instanceof PercentageLiteral) return new PercentageLiteral(((PercentageLiteral) lit).value);
        if (lit instanceof ScalarLiteral) return new ScalarLiteral(((ScalarLiteral) lit).value);
        if (lit instanceof BoolLiteral) return new BoolLiteral(((BoolLiteral) lit).value);
        if (lit instanceof ColorLiteral) return new ColorLiteral(((ColorLiteral) lit).value);
        return null;
    }
    private void expandIfClauseInto(IfClause clause, ArrayList<ASTNode> out) {
        Literal cond = evalToLiteral(clause.conditionalExpression);
        boolean truthy = (cond instanceof BoolLiteral) && ((BoolLiteral) cond).value;

        variableValues.insert(0, new HashMap<>());

        ArrayList<ASTNode> chosen = new ArrayList<>();
        if (truthy) {
            if (clause.body != null) chosen.addAll(clause.body);
        } else if (clause.elseClause != null && clause.elseClause.body != null) {
            chosen.addAll(clause.elseClause.body);
        }

        for (ASTNode n : chosen) {
            if (n instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) n);
            } else if (n instanceof Declaration) {
                Declaration d = (Declaration) n;
                Literal lit = evalToLiteral(d.expression);
                if (lit != null) {
                    d.expression = lit;
                    out.add(d);
                }
            } else if (n instanceof IfClause) {
                expandIfClauseInto((IfClause) n, out);
            }
        }

        variableValues.delete(0);
    }
}