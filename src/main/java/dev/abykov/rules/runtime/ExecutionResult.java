package dev.abykov.rules.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExecutionResult(
        List<String> matchedRules,
        Map<String, Object> values) {

    public ExecutionResult {
        matchedRules = List.copyOf(matchedRules);

        values = Collections.unmodifiableMap(
                new LinkedHashMap<>(values)
        );
    }
}
