package dev.abykov.rules.parser;

import dev.abykov.rules.ast.*;
import dev.abykov.rules.grammar.RuleLanguageParser;
import dev.abykov.rules.grammar.RuleLanguageParserBaseVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigDecimal;

final class AstBuilder extends RuleLanguageParserBaseVisitor<Expression> {

    public Program buildProgram(RuleLanguageParser.ProgramContext ctx) {
        var rules = ctx.ruleDeclaration().stream()
                .map(this::buildRule)
                .toList();

        return new Program(rules);
    }

    private Rule buildRule(RuleLanguageParser.RuleDeclarationContext ctx) {
        return new Rule(
                decodeString(ctx.STRING().getText()),
                visit(ctx.condition()),
                buildAssignment(ctx.assignment())
        );
    }

    @Override
    public Expression visitCondition(RuleLanguageParser.ConditionContext ctx) {
        return visit(ctx.orExpression());
    }

    @Override
    public Expression visitOrExpression(RuleLanguageParser.OrExpressionContext ctx) {
        var operands = ctx.andExpression().stream()
                .map(this::visit)
                .toList();

        return operands.size() == 1
                ? operands.getFirst()
                : new Or(operands);
    }

    @Override
    public Expression visitAndExpression(RuleLanguageParser.AndExpressionContext ctx) {
        var operands = ctx.notExpression().stream()
                .map(this::visit)
                .toList();

        return operands.size() == 1
                ? operands.getFirst()
                : new And(operands);
    }

    @Override
    public Expression visitNotExpression(RuleLanguageParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return new Not(visit(ctx.notExpression()));
        }

        return visit(ctx.primaryExpression());
    }

    @Override
    public Expression visitPrimaryExpression(RuleLanguageParser.PrimaryExpressionContext ctx) {
        if (ctx.condition() != null) {
            return visit(ctx.condition());
        }

        return visit(ctx.comparison());
    }

    @Override
    public Comparison visitComparison(RuleLanguageParser.ComparisonContext ctx) {
        return new Comparison(
                buildProperty(ctx.property()),
                resolveOperator(ctx.comparisonOperator()),
                buildLiteral(ctx.literal())
        );
    }

    private Assignment buildAssignment(RuleLanguageParser.AssignmentContext ctx) {
        return new Assignment(
                buildProperty(ctx.property()),
                buildLiteral(ctx.literal())
        );
    }

    private Property buildProperty(RuleLanguageParser.PropertyContext ctx) {
        var path = ctx.IDENTIFIER().stream()
                .map(TerminalNode::getText)
                .toList();

        return new Property(path);
    }

    private Literal buildLiteral(RuleLanguageParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) {
            return new NumberLiteral(
                    new BigDecimal(ctx.NUMBER().getText())
            );
        }

        if (ctx.STRING() != null) {
            return new StringLiteral(
                    decodeString(ctx.STRING().getText())
            );
        }

        if (ctx.TRUE() != null) {
            return new BooleanLiteral(true);
        }

        if (ctx.FALSE() != null) {
            return new BooleanLiteral(false);
        }

        throw new IllegalArgumentException(
                "Unsupported literal: " + ctx.getText()
        );
    }

    private ComparisonOperator resolveOperator(
            RuleLanguageParser.ComparisonOperatorContext ctx) {

        return switch (ctx.getText()) {
            case "==" -> ComparisonOperator.EQ;
            case "!=" -> ComparisonOperator.NE;
            case ">" -> ComparisonOperator.GT;
            case ">=" -> ComparisonOperator.GE;
            case "<" -> ComparisonOperator.LT;
            case "<=" -> ComparisonOperator.LE;

            default -> throw new IllegalArgumentException(
                    "Unsupported operator: " + ctx.getText()
            );
        };
    }

    private String decodeString(String text) {
        var result = new StringBuilder();

        for (int i = 1; i < text.length() - 1; i++) {
            char ch = text.charAt(i);

            if (ch == '\\') {
                ch = text.charAt(++i);
            }

            result.append(ch);
        }

        return result.toString();
    }
}
