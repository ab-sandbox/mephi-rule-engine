package dev.abykov.rules.parser;

import java.util.List;
import java.util.stream.Collectors;

public class RuleSyntaxException extends RuntimeException {

    private final List<SyntaxError> errors;

    public RuleSyntaxException(List<SyntaxError> errors) {
        super(formatMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    private static String formatMessage(List<SyntaxError> errors) {
        return errors.stream()
                .map(error -> "Line %d:%d — %s".formatted(
                        error.line(),
                        error.column(),
                        error.message()
                ))
                .collect(Collectors.joining(
                        System.lineSeparator()
                ));
    }
}
