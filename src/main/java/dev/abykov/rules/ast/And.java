package dev.abykov.rules.ast;

import java.util.List;

public record And(List<Expression> operands) implements Expression {

    public And {
        operands = List.copyOf(operands);
    }
}
