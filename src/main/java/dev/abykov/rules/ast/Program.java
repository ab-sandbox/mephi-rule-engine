package dev.abykov.rules.ast;

import java.util.List;

public record Program(List<Rule> rules) {

    public Program {
        rules = List.copyOf(rules);
    }
}
