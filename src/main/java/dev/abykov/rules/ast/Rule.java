package dev.abykov.rules.ast;

public record Rule(String name, Expression condition, Assignment assignment) {
}
