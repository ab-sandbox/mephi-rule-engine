package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.*;

import java.math.BigDecimal;
import java.util.Objects;

public final class ExpressionEvaluator {

    public boolean evaluate(
            Expression expression,
            EvaluationContext context) {

        Objects.requireNonNull(expression);
        Objects.requireNonNull(context);

        return switch (expression) {
            case Comparison comparison -> evaluateComparison(comparison, context);

            case And and -> and.operands().stream()
                    .allMatch(operand ->
                            evaluate(operand, context));

            case Or or -> or.operands().stream()
                    .anyMatch(operand ->
                            evaluate(operand, context));

            case Not not -> !evaluate(not.operand(), context);
        };
    }

    private boolean evaluateComparison(
            Comparison comparison,
            EvaluationContext context) {

        Object actual = context.resolve(comparison.property());

        return switch (comparison.literal()) {
            case NumberLiteral literal -> compareNumbers(
                    actual,
                    literal.value(),
                    comparison.operator()
            );

            case StringLiteral literal -> compareStrings(
                    actual,
                    literal.value(),
                    comparison.operator()
            );

            case BooleanLiteral literal -> compareBooleans(
                    actual,
                    literal.value(),
                    comparison.operator()
            );
        };
    }

    private boolean compareNumbers(
            Object actual,
            BigDecimal expected,
            ComparisonOperator operator) {

        if (!(actual instanceof Number number)) {
            throw typeMismatch("number", actual);
        }

        BigDecimal value;

        try {
            value = new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new EvaluationException(
                    "Invalid numeric value: " + number
            );
        }

        int result = value.compareTo(expected);

        return switch (operator) {
            case EQ -> result == 0;
            case NE -> result != 0;
            case GT -> result > 0;
            case GE -> result >= 0;
            case LT -> result < 0;
            case LE -> result <= 0;
        };
    }

    private boolean compareStrings(
            Object actual,
            String expected,
            ComparisonOperator operator) {

        if (!(actual instanceof String value)) {
            throw typeMismatch("string", actual);
        }

        return compareEquality(
                value.equals(expected),
                operator
        );
    }

    private boolean compareBooleans(
            Object actual,
            boolean expected,
            ComparisonOperator operator) {

        if (!(actual instanceof Boolean value)) {
            throw typeMismatch("boolean", actual);
        }

        return compareEquality(
                value == expected,
                operator
        );
    }

    private boolean compareEquality(
            boolean equal,
            ComparisonOperator operator) {

        return switch (operator) {
            case EQ -> equal;
            case NE -> !equal;

            default -> throw new EvaluationException(
                    "Unsupported comparison operator: " + operator
            );
        };
    }

    private EvaluationException typeMismatch(
            String expected,
            Object actual) {

        String actualType = actual == null
                ? "null"
                : actual.getClass().getSimpleName();

        return new EvaluationException(
                "Expected " + expected + ", but got " + actualType
        );
    }
}
