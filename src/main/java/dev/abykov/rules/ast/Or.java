package dev.abykov.rules.ast;

import java.util.List;

public record Or(List<Expression> operands) implements Expression {

    public Or {
        operands = List.copyOf(operands);
    }
}
