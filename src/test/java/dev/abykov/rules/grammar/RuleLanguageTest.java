package dev.abykov.rules.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleLanguageTest {

    @Test
    void shouldInspectTokens() {
        var lexer = new RuleLanguageLexer(
                CharStreams.fromString("order.total >= 5000"));

        var tokens = lexer.getAllTokens();

        printTokens(lexer, tokens);

        assertEquals(
                List.of(
                        RuleLanguageLexer.IDENTIFIER,
                        RuleLanguageLexer.DOT,
                        RuleLanguageLexer.IDENTIFIER,
                        RuleLanguageLexer.GE,
                        RuleLanguageLexer.NUMBER
                ),
                tokens.stream()
                        .map(Token::getType)
                        .toList()
        );

        assertEquals(
                List.of("order", ".", "total", ">=", "5000"),
                tokens.stream()
                        .map(Token::getText)
                        .toList()
        );
    }

    @Test
    void shouldParseSingleRule() {
        var parser = createParser("""
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());
        assertEquals(1, program.ruleDeclaration().size());

        var rule = program.ruleDeclaration(0);

        assertEquals("\"Free delivery\"", rule.STRING().getText());

        var condition = rule.condition();

        assertEquals("order.total", condition.property().getText());
        assertEquals(">=", condition.comparisonOperator().getText());
        assertEquals("5000", condition.literal().getText());

        var assignment = rule.assignment();

        assertEquals("delivery", assignment.property().getText());
        assertEquals("0", assignment.literal().getText());
    }

    @Test
    void shouldParseMultipleRules() {
        var parser = createParser("""
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());
        assertEquals(2, program.ruleDeclaration().size());

        assertEquals(
                List.of("\"Free delivery\"", "\"Premium discount\""),
                program.ruleDeclaration().stream()
                        .map(rule -> rule.STRING().getText())
                        .toList()
        );
    }

    @Test
    void shouldParseNestedProperty() {
        var parser = createParser("""
                rule "Customer location" {
                    when customer.address.city == "Moscow"
                    then discount = 10
                }
                """);

        var program = parseProgram(parser);
        var condition = program.ruleDeclaration(0).condition();

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        assertEquals(
                "customer.address.city",
                condition.property().getText()
        );

        assertEquals(
                List.of("customer", "address", "city"),
                condition.property().IDENTIFIER().stream()
                        .map(ParseTree::getText)
                        .toList()
        );

        assertEquals("\"Moscow\"", condition.literal().getText());
    }

    @Test
    void shouldRejectMissingCondition() {
        var parser = createParser("""
                rule "Invalid rule" {
                    then discount = 10
                }
                """);

        parseProgram(parser);

        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

    @Test
    void shouldRejectEmptyProgram() {
        var parser = createParser("");

        parseProgram(parser);

        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

    private RuleLanguageParser createParser(String source) {
        var lexer = new RuleLanguageLexer(
                CharStreams.fromString(source));

        var tokens = new CommonTokenStream(lexer);

        tokens.fill();

        System.out.println("\nSource:");
        System.out.println(source);

        printTokens(lexer, tokens.getTokens());

        var parser = new RuleLanguageParser(tokens);

        parser.removeErrorListeners();

        return parser;
    }

    private RuleLanguageParser.ProgramContext parseProgram(
            RuleLanguageParser parser) {

        var program = parser.program();

        System.out.println("\nParse tree:");
        System.out.println(program.toStringTree(parser));

        System.out.printf(
                "Syntax errors: %d%n",
                parser.getNumberOfSyntaxErrors()
        );

        return program;
    }

    private void printTokens(
            RuleLanguageLexer lexer,
            List<? extends Token> tokens) {

        var vocabulary = lexer.getVocabulary();

        System.out.println("\nTokens:");
        System.out.printf(
                "%-15s %-20s %s%n",
                "TYPE", "TEXT", "POSITION"
        );

        for (var token : tokens) {
            System.out.printf(
                    "%-15s %-20s %d:%d%n",
                    vocabulary.getDisplayName(token.getType()),
                    token.getText(),
                    token.getLine(),
                    token.getCharPositionInLine()
            );
        }
    }
}
