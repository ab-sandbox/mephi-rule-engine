package dev.abykov.rules.parser;

import dev.abykov.rules.ast.Comparison;
import dev.abykov.rules.ast.ComparisonOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleParserTest {

    private final RuleParser parser = new RuleParser();

    @Test
    void shouldParseValidProgram() {
        var program = parser.parse("""
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        assertEquals(1, program.rules().size());

        var rule = program.rules().getFirst();

        assertEquals("Free delivery", rule.name());

        var comparison = assertInstanceOf(
                Comparison.class,
                rule.condition()
        );

        assertEquals(
                ComparisonOperator.GE,
                comparison.operator()
        );

        System.out.println("AST:");
        System.out.println(program);
    }

    @Test
    void shouldRejectMissingCondition() {
        assertInvalid("""
                rule "Invalid rule" {
                    then discount = 10
                }
                """);
    }

    @Test
    void shouldRejectUnclosedParenthesis() {
        assertInvalid("""
                rule "Invalid rule" {
                    when (customer.age >= 18
                    then discount = 15
                }
                """);
    }

    @Test
    void shouldRejectInvalidCharacter() {
        assertInvalid("""
                rule "Invalid rule" {
                    when order.total >= @5000
                    then delivery = 0
                }
                """);
    }

    @Test
    void shouldRejectEmptyProgram() {
        assertInvalid("");
    }

    @Test
    void shouldRejectNullSource() {
        assertThrows(
                NullPointerException.class,
                () -> parser.parse(null)
        );
    }

    @Test
    void shouldNotRetainErrorsBetweenInvocations() {
        assertInvalid("");

        var program = parser.parse("""
                rule "Valid rule" {
                    when customer.age >= 18
                    then allowed = true
                }
                """);

        assertEquals(1, program.rules().size());
    }

    private void assertInvalid(String source) {
        var exception = assertThrows(
                RuleSyntaxException.class,
                () -> parser.parse(source)
        );

        assertFalse(exception.getErrors().isEmpty());

        System.out.println("\nSource:");
        System.out.println(source);

        System.out.println("Syntax errors:");
        System.out.println(exception.getMessage());
    }
}
