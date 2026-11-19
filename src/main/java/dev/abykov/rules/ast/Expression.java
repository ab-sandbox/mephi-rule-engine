package dev.abykov.rules.ast;

public sealed interface Expression
        permits Comparison, And, Or, Not {
}
