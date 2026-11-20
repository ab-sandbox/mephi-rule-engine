package dev.abykov.rules.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ContextSnapshot {

    private ContextSnapshot() {
    }

    static Map<String, Object> copy(Map<String, Object> source) {
        var result = new LinkedHashMap<String, Object>();

        source.forEach((key, value) ->
                result.put(key, copyValue(value)));

        return Collections.unmodifiableMap(result);
    }

    private static Object copyValue(Object value) {
        return switch (value) {
            case Map<?, ?> map -> copyMap(map);

            case List<?> list -> list.stream()
                    .map(ContextSnapshot::copyValue)
                    .toList();

            case null -> null;

            default -> value;
        };
    }

    private static Map<?, ?> copyMap(Map<?, ?> source) {
        var result = new LinkedHashMap<Object, Object>();

        source.forEach((key, value) ->
                result.put(key, copyValue(value)));

        return Collections.unmodifiableMap(result);
    }
}
