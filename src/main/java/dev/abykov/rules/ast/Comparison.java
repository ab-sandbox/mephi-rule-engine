package dev.abykov.rules.ast;

public record Comparison(Property property, ComparisonOperator operator, Literal literal) implements Expression {
}
