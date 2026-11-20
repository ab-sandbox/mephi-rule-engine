package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.Program;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class RuleEngine {

    private final ExpressionEvaluator expressionEvaluator;
    private final LiteralEvaluator literalEvaluator;

    public RuleEngine() {
        this.expressionEvaluator = new ExpressionEvaluator();
        this.literalEvaluator = new LiteralEvaluator();
    }

    public ExecutionResult execute(
            Program program,
            EvaluationContext context) {

        Objects.requireNonNull(program);
        Objects.requireNonNull(context);

        var matchedRules = new ArrayList<String>();
        var values = new LinkedHashMap<String, Object>();

        for (var rule : program.rules()) {

            if (!expressionEvaluator.evaluate(
                    rule.condition(), context)) {
                continue;
            }

            var assignment = rule.assignment();

            String property = String.join(
                    ".",
                    assignment.property().path()
            );

            Object value = literalEvaluator.evaluate(
                    assignment.literal()
            );

            matchedRules.add(rule.name());
            values.put(property, value);
        }

        return new ExecutionResult(
                matchedRules,
                values
        );
    }
}
