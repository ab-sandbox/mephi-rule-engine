package dev.abykov.rules.parser;

import dev.abykov.rules.ast.*;
import dev.abykov.rules.grammar.RuleLanguageLexer;
import dev.abykov.rules.grammar.RuleLanguageParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstBuilderTest {

    @Test
    void shouldBuildSimpleRule() {
        var program = build("""
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        var expected = new Program(List.of(
                new Rule(
                        "Free delivery",
                        new Comparison(
                                new Property(List.of("order", "total")),
                                ComparisonOperator.GE,
                                new NumberLiteral(new BigDecimal("5000"))
                        ),
                        new Assignment(
                                new Property(List.of("delivery")),
                                new NumberLiteral(BigDecimal.ZERO)
                        )
                )
        ));

        assertEquals(expected, program);
    }

    @Test
    void shouldBuildAndExpression() {
        var program = build("""
                rule "Premium discount" {
                    when customer.age >= 18
                        and customer.premium == true
                    then discount = 15
                }
                """);

        var expected = new And(List.of(
                new Comparison(
                        new Property(List.of("customer", "age")),
                        ComparisonOperator.GE,
                        new NumberLiteral(new BigDecimal("18"))
                ),
                new Comparison(
                        new Property(List.of("customer", "premium")),
                        ComparisonOperator.EQ,
                        new BooleanLiteral(true)
                )
        ));

        assertEquals(
                expected,
                program.rules().getFirst().condition()
        );
    }

    @Test
    void shouldRespectOperatorPrecedence() {
        var program = build("""
                rule "Special discount" {
                    when customer.premium == true
                        or customer.age >= 18
                        and order.total >= 10000
                    then discount = 20
                }
                """);

        var or = assertInstanceOf(
                Or.class,
                program.rules().getFirst().condition()
        );

        assertEquals(2, or.operands().size());

        assertInstanceOf(
                Comparison.class,
                or.operands().getFirst()
        );

        var and = assertInstanceOf(
                And.class,
                or.operands().getLast()
        );

        assertEquals(2, and.operands().size());
    }

    @Test
    void shouldRespectParentheses() {
        var program = build("""
                rule "Special discount" {
                    when (customer.premium == true
                        or customer.age >= 18)
                        and order.total >= 10000
                    then discount = 20
                }
                """);

        var and = assertInstanceOf(
                And.class,
                program.rules().getFirst().condition()
        );

        assertEquals(2, and.operands().size());

        var or = assertInstanceOf(
                Or.class,
                and.operands().getFirst()
        );

        assertEquals(2, or.operands().size());
    }

    @Test
    void shouldBuildDoubleNegation() {
        var program = build("""
                rule "Double negation" {
                    when not not customer.blocked == true
                    then allowed = true
                }
                """);

        var outer = assertInstanceOf(
                Not.class,
                program.rules().getFirst().condition()
        );

        var inner = assertInstanceOf(
                Not.class,
                outer.operand()
        );

        assertInstanceOf(
                Comparison.class,
                inner.operand()
        );
    }

    @Test
    void shouldDecodeStringLiteral() {
        var program = build("""
                rule "Customer location" {
                    when customer.address.city == "Moscow"
                    then discount = 10
                }
                """);

        var comparison = assertInstanceOf(
                Comparison.class,
                program.rules().getFirst().condition()
        );

        assertEquals(
                new StringLiteral("Moscow"),
                comparison.literal()
        );
    }

    @Test
    void shouldDecodeEscapedStringLiteral() {
        var program = build("""
                rule "Greeting" {
                    when message == "Hello \\"world\\""
                    then accepted = true
                }
                """);

        var comparison = assertInstanceOf(
                Comparison.class,
                program.rules().getFirst().condition()
        );

        assertEquals(
                new StringLiteral("Hello \"world\""),
                comparison.literal()
        );
    }

    private Program build(String source) {
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

        assertFalse(
                errorListener.hasErrors(),
                () -> errorListener.getErrors().toString()
        );

        var program = new AstBuilder().buildProgram(tree);

        System.out.println("\nSource:");
        System.out.println(source);

        System.out.println("Parse tree:");
        System.out.println(tree.toStringTree(parser));

        System.out.println("\nAST:");
        System.out.println(program);

        return program;
    }
}
