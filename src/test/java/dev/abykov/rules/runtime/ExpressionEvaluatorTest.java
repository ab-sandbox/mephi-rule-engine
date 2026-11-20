package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.abykov.rules.ast.ComparisonOperator.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorTest {

    private final ExpressionEvaluator evaluator =
            new ExpressionEvaluator();

    private final EvaluationContext context =
            new EvaluationContext(Map.of(
                    "customer", Map.of(
                            "age", 25,
                            "premium", true,
                            "blocked", false,
                            "city", "Moscow"
                    ),
                    "order", Map.of(
                            "total", new BigDecimal("15000.00")
                    )
            ));

    @Test
    void shouldEvaluateNumericEquality() {
        assertEvaluated(true,
                number(EQ, "15000", "order", "total"));
    }

    @Test
    void shouldEvaluateNumericInequality() {
        assertEvaluated(true,
                number(NE, "10000", "order", "total"));
    }

    @Test
    void shouldEvaluateGreaterThan() {
        assertEvaluated(true,
                number(GT, "18", "customer", "age"));
    }

    @Test
    void shouldEvaluateGreaterThanOrEqual() {
        assertEvaluated(true,
                number(GE, "25", "customer", "age"));
    }

    @Test
    void shouldEvaluateLessThan() {
        assertEvaluated(true,
                number(LT, "30", "customer", "age"));
    }

    @Test
    void shouldEvaluateLessThanOrEqual() {
        assertEvaluated(true,
                number(LE, "25", "customer", "age"));
    }

    @Test
    void shouldEvaluateFalseComparison() {
        assertEvaluated(false,
                number(GT, "30", "customer", "age"));
    }

    @Test
    void shouldEvaluateStringEquality() {
        assertEvaluated(true,
                comparison(
                        EQ,
                        new StringLiteral("Moscow"),
                        "customer", "city"
                ));
    }

    @Test
    void shouldEvaluateStringInequality() {
        assertEvaluated(true,
                comparison(
                        NE,
                        new StringLiteral("London"),
                        "customer", "city"
                ));
    }

    @Test
    void shouldEvaluateBooleanEquality() {
        assertEvaluated(true,
                bool(EQ, true, "customer", "premium"));
    }

    @Test
    void shouldEvaluateBooleanInequality() {
        assertEvaluated(true,
                bool(NE, true, "customer", "blocked"));
    }

    @Test
    void shouldEvaluateAnd() {
        var expression = new And(List.of(
                number(GE, "18", "customer", "age"),
                bool(EQ, true, "customer", "premium")
        ));

        assertEvaluated(true, expression);
    }

    @Test
    void shouldEvaluateOr() {
        var expression = new Or(List.of(
                number(GT, "30", "customer", "age"),
                bool(EQ, true, "customer", "premium")
        ));

        assertEvaluated(true, expression);
    }

    @Test
    void shouldEvaluateNot() {
        var expression = new Not(
                bool(EQ, true, "customer", "blocked")
        );

        assertEvaluated(true, expression);
    }

    @Test
    void shouldEvaluateNestedExpression() {
        var expression = new And(List.of(
                new Or(List.of(
                        number(GE, "30", "customer", "age"),
                        bool(EQ, true, "customer", "premium")
                )),
                new Not(
                        bool(EQ, true, "customer", "blocked")
                )
        ));

        assertEvaluated(true, expression);
    }

    @Test
    void shouldShortCircuitAnd() {
        var expression = new And(List.of(
                number(GT, "30", "customer", "age"),
                number(EQ, "100", "unknown")
        ));

        assertEvaluated(false, expression);
    }

    @Test
    void shouldShortCircuitOr() {
        var expression = new Or(List.of(
                bool(EQ, true, "customer", "premium"),
                number(EQ, "100", "unknown")
        ));

        assertEvaluated(true, expression);
    }

    @Test
    void shouldRejectTypeMismatch() {
        var expression = comparison(
                EQ,
                new StringLiteral("25"),
                "customer", "age"
        );

        assertInvalid(expression);
    }

    @Test
    void shouldRejectStringOrdering() {
        var expression = comparison(
                GT,
                new StringLiteral("London"),
                "customer", "city"
        );

        assertInvalid(expression);
    }

    @Test
    void shouldRejectBooleanOrdering() {
        assertInvalid(
                bool(GT, true, "customer", "premium")
        );
    }

    @Test
    void shouldRejectUnknownProperty() {
        assertInvalid(number(EQ, "100", "unknown"));
    }

    @Test
    void shouldRejectNullValue() {
        var data = new HashMap<String, Object>();
        data.put("value", null);

        var nullableContext = new EvaluationContext(data);

        var expression = number(EQ, "100", "value");

        assertThrows(
                EvaluationException.class,
                () -> evaluator.evaluate(
                        expression,
                        nullableContext
                )
        );
    }

    private Comparison number(
            ComparisonOperator operator,
            String value,
            String... path) {

        return comparison(
                operator,
                new NumberLiteral(new BigDecimal(value)),
                path
        );
    }

    private Comparison bool(
            ComparisonOperator operator,
            boolean value,
            String... path) {

        return comparison(
                operator,
                new BooleanLiteral(value),
                path
        );
    }

    private Comparison comparison(
            ComparisonOperator operator,
            Literal literal,
            String... path) {

        return new Comparison(
                new Property(List.of(path)),
                operator,
                literal
        );
    }

    private void assertEvaluated(
            boolean expected,
            Expression expression) {

        boolean actual = evaluator.evaluate(
                expression,
                context
        );

        assertEquals(expected, actual);

        System.out.printf(
                "%s%nResult: %s%n%n",
                expression,
                actual
        );
    }

    private void assertInvalid(Expression expression) {
        var exception = assertThrows(
                EvaluationException.class,
                () -> evaluator.evaluate(expression, context)
        );

        System.out.println(exception.getMessage());
    }
}
