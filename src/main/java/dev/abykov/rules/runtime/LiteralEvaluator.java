package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.*;

final class LiteralEvaluator {

    Object evaluate(Literal literal) {
        return switch (literal) {
            case NumberLiteral number -> number.value();
            case StringLiteral string -> string.value();
            case BooleanLiteral bool -> bool.value();
        };
    }
}
