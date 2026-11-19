package dev.abykov.rules.ast;

import java.math.BigDecimal;

public record NumberLiteral(BigDecimal value) implements Literal {
}
