package dev.abykov.rules.json;

import dev.abykov.rules.ast.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonContextLoaderTest {

    private final JsonContextLoader loader =
            new JsonContextLoader();

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadNestedProperties() throws IOException {
        var context = loader.parse("""
                {
                  "customer": {
                    "address": {
                      "city": "Moscow"
                    },
                    "premium": true
                  }
                }
                """);

        assertEquals(
                "Moscow",
                context.resolve(property(
                        "customer", "address", "city"
                ))
        );

        assertEquals(
                true,
                context.resolve(property("customer", "premium"))
        );
    }

    @Test
    void shouldPreserveDecimalPrecision() throws IOException {
        var context = loader.parse("""
                {
                  "total": 1234567890.123456789
                }
                """);

        assertEquals(
                new BigDecimal("1234567890.123456789"),
                context.resolve(property("total"))
        );
    }

    @Test
    void shouldPreserveLargeInteger() throws IOException {
        var context = loader.parse("""
                {
                  "value": 123456789012345678901234567890
                }
                """);

        assertEquals(
                new BigInteger("123456789012345678901234567890"),
                context.resolve(property("value"))
        );
    }

    @Test
    void shouldLoadContextFromFile() throws IOException {
        var file = tempDir.resolve("context.json");

        Files.writeString(file, """
                {"premium": true}
                """);

        var context = loader.load(file);

        assertEquals(
                true,
                context.resolve(property("premium"))
        );
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThrows(
                IOException.class,
                () -> loader.parse("{invalid}")
        );
    }

    @Test
    void shouldRejectArrayAsRoot() {
        assertThrows(
                IOException.class,
                () -> loader.parse("[1, 2, 3]")
        );
    }

    @Test
    void shouldRejectNullAsRoot() {
        assertThrows(
                IOException.class,
                () -> loader.parse("null")
        );
    }

    @Test
    void shouldRejectTrailingJson() {
        assertThrows(
                IOException.class,
                () -> loader.parse("{} {}")
        );
    }

    private Property property(String... path) {
        return new Property(List.of(path));
    }
}
