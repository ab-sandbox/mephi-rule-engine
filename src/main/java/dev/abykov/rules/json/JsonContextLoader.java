package dev.abykov.rules.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abykov.rules.runtime.EvaluationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class JsonContextLoader {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public EvaluationContext load(Path path) throws IOException {
        return parse(Files.readString(path));
    }

    public EvaluationContext parse(String json) throws IOException {
        Map<String, Object> data = mapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {
                }
        );

        if (data == null) {
            throw new IOException("JSON context must be an object");
        }

        return new EvaluationContext(data);
    }
}
