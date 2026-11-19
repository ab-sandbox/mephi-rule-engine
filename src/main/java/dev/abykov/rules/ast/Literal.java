package dev.abykov.rules.ast;

public sealed interface Literal
        permits NumberLiteral, StringLiteral, BooleanLiteral {
}
