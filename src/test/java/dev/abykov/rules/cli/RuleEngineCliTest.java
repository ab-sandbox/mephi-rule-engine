package dev.abykov.rules.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.abykov.rules.cli.RuleEngineCli.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineCliTest {

    private final RuleEngineCli cli = new RuleEngineCli();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ByteArrayOutputStream stdout =
            new ByteArrayOutputStream();

    private final ByteArrayOutputStream stderr =
            new ByteArrayOutputStream();

    @TempDir
    Path tempDir;

    private Path rulesFile;
    private Path contextFile;

    @BeforeEach
    void setUp() throws IOException {
        rulesFile = tempDir.resolve("discount.rules");
        contextFile = tempDir.resolve("context.json");

        Files.writeString(rulesFile, """
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        Files.writeString(contextFile, """
                {
                  "customer": {
                    "premium": true
                  }
                }
                """);
    }

    @Test
    void shouldExecuteRulesFromFiles() throws IOException {
        assertEquals(EXIT_SUCCESS, executeFiles());

        var result = mapper.readTree(output());

        assertEquals(
                "Premium discount",
                result.path("matchedRules").get(0).asText()
        );

        assertEquals(
                15,
                result.path("values").path("discount").asInt()
        );

        assertTrue(errors().isEmpty());

        System.out.println(output());
    }

    @Test
    void shouldPrintHelp() {
        assertEquals(EXIT_SUCCESS, run("--help"));

        assertTrue(output().contains("Usage:"));
        assertTrue(errors().isEmpty());
    }

    @Test
    void shouldRejectMissingArguments() {
        assertEquals(EXIT_INVALID_ARGUMENTS, run());

        assertTrue(errors().contains("Usage:"));
        assertTrue(output().isEmpty());
    }

    @Test
    void shouldReportSyntaxError() throws IOException {
        Files.writeString(rulesFile, """
                rule "Invalid" {
                    then discount = 15
                }
                """);

        assertEquals(EXIT_SYNTAX_ERROR, executeFiles());

        assertTrue(errors().contains("Syntax error:"));
        assertTrue(output().isEmpty());
    }

    @Test
    void shouldReportEvaluationError() throws IOException {
        Files.writeString(contextFile, "{}");

        assertEquals(EXIT_EVALUATION_ERROR, executeFiles());

        assertTrue(errors().contains("Evaluation error:"));
        assertTrue(output().isEmpty());
    }

    @Test
    void shouldReportMalformedJson() throws IOException {
        Files.writeString(contextFile, "{invalid}");

        assertEquals(EXIT_IO_ERROR, executeFiles());

        assertTrue(errors().contains("I/O error:"));
        assertTrue(output().isEmpty());
    }

    @Test
    void shouldReportMissingFile() throws IOException {
        Files.delete(rulesFile);

        assertEquals(EXIT_IO_ERROR, executeFiles());

        assertTrue(errors().contains("I/O error:"));
        assertTrue(output().isEmpty());
    }

    @Test
    void shouldPreserveExitCodeContract() {
        assertAll(
                () -> assertEquals(0, EXIT_SUCCESS),
                () -> assertEquals(2, EXIT_INVALID_ARGUMENTS),
                () -> assertEquals(3, EXIT_SYNTAX_ERROR),
                () -> assertEquals(4, EXIT_EVALUATION_ERROR),
                () -> assertEquals(5, EXIT_IO_ERROR)
        );
    }

    private int executeFiles() {
        return run(
                rulesFile.toString(),
                contextFile.toString()
        );
    }

    private int run(String... args) {
        try (var out = new PrintStream(
                stdout, true, StandardCharsets.UTF_8);
             var err = new PrintStream(
                     stderr, true, StandardCharsets.UTF_8)) {

            return cli.run(args, out, err);
        }
    }

    private String output() {
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private String errors() {
        return stderr.toString(StandardCharsets.UTF_8);
    }
}
