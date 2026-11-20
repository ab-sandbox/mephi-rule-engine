package dev.abykov.rules.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abykov.rules.json.JsonContextLoader;
import dev.abykov.rules.parser.RuleParser;
import dev.abykov.rules.parser.RuleSyntaxException;
import dev.abykov.rules.runtime.EvaluationException;
import dev.abykov.rules.runtime.RuleEngine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class RuleEngineCli {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_INVALID_ARGUMENTS = 2;
    public static final int EXIT_SYNTAX_ERROR = 3;
    public static final int EXIT_EVALUATION_ERROR = 4;
    public static final int EXIT_IO_ERROR = 5;

    private static final int REQUIRED_ARGS_COUNT = 2;
    private static final int HELP_ARGS_COUNT = 1;

    private static final int RULES_FILE_INDEX = 0;
    private static final int CONTEXT_FILE_INDEX = 1;

    private static final String HELP_OPTION = "--help";
    private static final String HELP_SHORT_OPTION = "-h";

    private final RuleParser parser = new RuleParser();
    private final RuleEngine engine = new RuleEngine();
    private final JsonContextLoader contextLoader =
            new JsonContextLoader();

    private final ObjectMapper mapper = new ObjectMapper();

    public int run(
            String[] args,
            PrintStream out,
            PrintStream err) {

        if (isHelpRequested(args)) {
            printUsage(out);
            return EXIT_SUCCESS;
        }

        if (args.length != REQUIRED_ARGS_COUNT) {
            printUsage(err);
            return EXIT_INVALID_ARGUMENTS;
        }

        try {
            var rulesPath = Path.of(args[RULES_FILE_INDEX]);
            var contextPath = Path.of(args[CONTEXT_FILE_INDEX]);

            var source = Files.readString(rulesPath);
            var program = parser.parse(source);

            var context = contextLoader.load(contextPath);
            var result = engine.execute(program, context);

            String json = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            out.println(json);

            return EXIT_SUCCESS;

        } catch (RuleSyntaxException e) {
            err.println("Syntax error: " + e.getMessage());
            return EXIT_SYNTAX_ERROR;

        } catch (EvaluationException e) {
            err.println("Evaluation error: " + e.getMessage());
            return EXIT_EVALUATION_ERROR;

        } catch (IOException e) {
            err.println("I/O error: " + e.getMessage());
            return EXIT_IO_ERROR;

        } catch (InvalidPathException e) {
            err.println("Invalid path: " + e.getMessage());
            return EXIT_INVALID_ARGUMENTS;
        }
    }

    private boolean isHelpRequested(String[] args) {
        return args.length == HELP_ARGS_COUNT &&
               (HELP_OPTION.equals(args[0]) ||
                HELP_SHORT_OPTION.equals(args[0]));
    }

    private void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println(
                "  java -jar rule-engine.jar <rules-file> <context-file>"
        );
        stream.println(
                "  java -jar rule-engine.jar --help"
        );
    }
}
