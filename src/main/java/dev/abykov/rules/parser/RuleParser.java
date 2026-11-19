package dev.abykov.rules.parser;

import dev.abykov.rules.ast.Program;
import dev.abykov.rules.grammar.RuleLanguageLexer;
import dev.abykov.rules.grammar.RuleLanguageParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Objects;

public final class RuleParser {

    public Program parse(String source) {
        Objects.requireNonNull(source, "Source must not be null");

        var errorListener = new CollectingErrorListener();

        var lexer = new RuleLanguageLexer(
                CharStreams.fromString(source)
        );

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        var tokens = new CommonTokenStream(lexer);

        var parser = new RuleLanguageParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        var tree = parser.program();

        if (errorListener.hasErrors()) {
            throw new RuleSyntaxException(
                    errorListener.getErrors()
            );
        }

        return new AstBuilder().buildProgram(tree);
    }
}
