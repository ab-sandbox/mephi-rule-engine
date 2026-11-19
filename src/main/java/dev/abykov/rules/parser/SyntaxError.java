package dev.abykov.rules.parser;

public record SyntaxError(
        int line,
        int column,
        String message
) {
}
