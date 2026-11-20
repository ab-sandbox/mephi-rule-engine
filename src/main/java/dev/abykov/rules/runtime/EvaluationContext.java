package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.Property;

import java.util.Map;
import java.util.Objects;

public final class EvaluationContext {

    private final Map<String, Object> data;

    public EvaluationContext(Map<String, Object> data) {
        this.data = ContextSnapshot.copy(
                Objects.requireNonNull(data)
        );
    }

    public Object resolve(Property property) {
        Objects.requireNonNull(property);

        Object current = data;

        String fullPath = String.join(".", property.path());

        for (String segment : property.path()) {

            if (!(current instanceof Map<?, ?> map)) {
                throw new EvaluationException(
                        "Cannot resolve property: " + fullPath
                );
            }

            if (!map.containsKey(segment)) {
                throw new EvaluationException(
                        "Property not found: " + fullPath
                );
            }

            current = map.get(segment);
        }

        return current;
    }
}
