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

        var condition = getComparison(rule.condition());

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
        var condition = getComparison(
                program.ruleDeclaration(0).condition()
        );

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
                    vocabulary.getSymbolicName(token.getType()),
                    token.getText(),
                    token.getLine(),
                    token.getCharPositionInLine()
            );
        }
    }

    @Test
    void shouldParseAndExpression() {
        var parser = createParser("""
                rule "Premium discount" {
                    when customer.age >= 18
                        and customer.premium == true
                    then discount = 15
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression()
                .andExpression(0);

        assertEquals(2, expression.notExpression().size());
        assertEquals(1, expression.AND().size());
    }

    @Test
    void shouldParseOrExpression() {
        var parser = createParser("""
                rule "Free delivery" {
                    when order.total >= 5000
                        or customer.premium == true
                    then delivery = 0
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression();

        assertEquals(2, expression.andExpression().size());
        assertEquals(1, expression.OR().size());
    }

    @Test
    void shouldParseNotExpression() {
        var parser = createParser("""
                rule "Active customer" {
                    when not customer.blocked == true
                    then allowed = true
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression()
                .andExpression(0)
                .notExpression(0);

        assertNotNull(expression.NOT());
        assertNotNull(expression.notExpression());

        assertNotNull(
                expression.notExpression()
                        .primaryExpression()
                        .comparison()
        );
    }

    @Test
    void shouldRespectOperatorPrecedence() {
        var parser = createParser("""
                rule "Special discount" {
                    when customer.premium == true
                        or customer.age >= 18
                        and order.total >= 10000
                    then discount = 20
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression();

        assertEquals(2, expression.andExpression().size());

        var left = expression.andExpression(0);
        var right = expression.andExpression(1);

        assertEquals(1, left.notExpression().size());
        assertEquals(2, right.notExpression().size());

        assertTrue(left.AND().isEmpty());
        assertEquals(1, right.AND().size());
    }

    @Test
    void shouldRespectParentheses() {
        var parser = createParser("""
                rule "Special discount" {
                    when (customer.premium == true
                        or customer.age >= 18)
                        and order.total >= 10000
                    then discount = 20
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression();

        assertEquals(1, expression.andExpression().size());

        var andExpression = expression.andExpression(0);

        assertEquals(2, andExpression.notExpression().size());

        var grouped = andExpression.notExpression(0)
                .primaryExpression();

        assertNotNull(grouped.LPAREN());
        assertNotNull(grouped.RPAREN());
        assertNotNull(grouped.condition());

        assertEquals(
                2,
                grouped.condition()
                        .orExpression()
                        .andExpression()
                        .size()
        );
    }

    @Test
    void shouldParseDoubleNegation() {
        var parser = createParser("""
                rule "Double negation" {
                    when not not customer.premium == true
                    then discount = 15
                }
                """);

        var program = parseProgram(parser);

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var expression = program.ruleDeclaration(0)
                .condition()
                .orExpression()
                .andExpression(0)
                .notExpression(0);

        assertNotNull(expression.NOT());
        assertNotNull(expression.notExpression().NOT());

        assertNotNull(
                expression.notExpression()
                        .notExpression()
                        .primaryExpression()
                        .comparison()
        );
    }

    @Test
    void shouldRejectUnclosedParenthesis() {
        var parser = createParser("""
                rule "Invalid rule" {
                    when (customer.age >= 18
                        and customer.premium == true
                    then discount = 15
                }
                """);

        parseProgram(parser);

        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

    private RuleLanguageParser.ComparisonContext getComparison(
            RuleLanguageParser.ConditionContext condition) {

        return condition
                .orExpression()
                .andExpression(0)
                .notExpression(0)
                .primaryExpression()
                .comparison();
    }
}
