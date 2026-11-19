package dev.abykov.rules.ast;

import java.util.List;

public record Property(List<String> path) {

    public Property {
        path = List.copyOf(path);
    }
}
