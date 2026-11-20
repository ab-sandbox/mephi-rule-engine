package dev.abykov.rules.runtime;

import dev.abykov.rules.parser.RuleParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private final RuleParser parser = new RuleParser();
    private final RuleEngine engine = new RuleEngine();

    private final EvaluationContext context =
            new EvaluationContext(Map.of(
                    "customer", Map.of(
                            "age", 25,
                            "premium", true
                    ),
                    "order", Map.of(
                            "total", 15000
                    )
            ));

    @Test
    void shouldExecuteMatchingRule() {
        var result = execute("""
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        assertEquals(
                List.of("Premium discount"),
                result.matchedRules()
        );

        assertEquals(
                Map.of("discount", new BigDecimal("15")),
                result.values()
        );
    }

    @Test
    void shouldSkipNonMatchingRule() {
        var result = execute("""
                rule "Senior discount" {
                    when customer.age >= 65
                    then discount = 20
                }
                """);

        assertTrue(result.matchedRules().isEmpty());
        assertTrue(result.values().isEmpty());
    }

    @Test
    void shouldExecuteMultipleMatchingRules() {
        var result = execute("""
                rule "Premium discount" {
                    when customer.age >= 18
                        and customer.premium == true
                    then discount = 15
                }
                
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        assertEquals(
                List.of("Premium discount", "Free delivery"),
                result.matchedRules()
        );

        assertEquals(
                Map.of(
                        "discount", new BigDecimal("15"),
                        "delivery", BigDecimal.ZERO
                ),
                result.values()
        );

        assertEquals(
                List.of("discount", "delivery"),
                List.copyOf(result.values().keySet())
        );
    }

    @Test
    void shouldExecuteOnlyMatchingRules() {
        var result = execute("""
                rule "Senior discount" {
                    when customer.age >= 65
                    then discount = 20
                }
                
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        assertEquals(
                List.of("Free delivery"),
                result.matchedRules()
        );

        assertEquals(
                Map.of("delivery", BigDecimal.ZERO),
                result.values()
        );
    }

    @Test
    void shouldApplyLastMatchingAssignment() {
        var result = execute("""
                rule "Standard discount" {
                    when customer.age >= 18
                    then discount = 5
                }
                
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        assertEquals(
                List.of("Standard discount", "Premium discount"),
                result.matchedRules()
        );

        assertEquals(
                new BigDecimal("15"),
                result.values().get("discount")
        );
    }

    @Test
    void shouldSupportStringAndBooleanAssignments() {
        var result = execute("""
                rule "Customer status" {
                    when customer.premium == true
                    then status = "VIP"
                }
                
                rule "Customer access" {
                    when customer.age >= 18
                    then allowed = true
                }
                """);

        assertEquals(
                Map.of(
                        "status", "VIP",
                        "allowed", true
                ),
                result.values()
        );
    }

    @Test
    void shouldSupportNestedAssignmentPath() {
        var result = execute("""
                rule "Order discount" {
                    when customer.premium == true
                    then order.discount = 15
                }
                """);

        assertEquals(
                Map.of(
                        "order.discount",
                        new BigDecimal("15")
                ),
                result.values()
        );
    }

    @Test
    void shouldNotModifyInputContext() {
        var result = execute("""
                rule "Update age" {
                    when customer.premium == true
                    then customer.age = 30
                }
                """);

        assertEquals(
                new BigDecimal("30"),
                result.values().get("customer.age")
        );

        var property = new dev.abykov.rules.ast.Property(
                List.of("customer", "age")
        );

        assertEquals(25, context.resolve(property));
    }

    @Test
    void shouldNotUseAssignmentsAsInputForOtherRules() {
        var result = execute("""
                rule "Update age" {
                    when customer.premium == true
                    then customer.age = 30
                }
                
                rule "Age discount" {
                    when customer.age >= 30
                    then discount = 10
                }
                """);

        assertEquals(
                List.of("Update age"),
                result.matchedRules()
        );

        assertFalse(result.values().containsKey("discount"));
    }

    @Test
    void shouldPropagateEvaluationError() {
        assertThrows(
                EvaluationException.class,
                () -> execute("""
                        rule "Invalid rule" {
                            when customer.salary >= 100000
                            then discount = 20
                        }
                        """)
        );
    }

    @Test
    void shouldReturnImmutableResult() {
        var result = execute("""
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.matchedRules().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.values().clear()
        );
    }

    @Test
    void shouldNotRetainStateBetweenExecutions() {
        execute("""
                rule "Premium discount" {
                    when customer.premium == true
                    then discount = 15
                }
                """);

        var result = execute("""
                rule "Free delivery" {
                    when order.total >= 5000
                    then delivery = 0
                }
                """);

        assertEquals(
                List.of("Free delivery"),
                result.matchedRules()
        );

        assertEquals(
                Map.of("delivery", BigDecimal.ZERO),
                result.values()
        );
    }

    private ExecutionResult execute(String source) {
        var program = parser.parse(source);

        var result = engine.execute(program, context);

        System.out.println("\nSource:");
        System.out.println(source);

        System.out.println("Matched rules:");
        System.out.println(result.matchedRules());

        System.out.println("Result:");
        System.out.println(result.values());

        return result;
    }
}
