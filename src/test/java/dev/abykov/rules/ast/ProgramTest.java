package dev.abykov.rules.ast;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgramTest {

    @Test
    void shouldBuildProgram() {
        var age = new Comparison(
                new Property(List.of("customer", "age")),
                ComparisonOperator.GE,
                new NumberLiteral(new BigDecimal("18"))
        );

        var premium = new Comparison(
                new Property(List.of("customer", "premium")),
                ComparisonOperator.EQ,
                new BooleanLiteral(true)
        );

        var condition = new And(List.of(age, premium));

        var assignment = new Assignment(
                new Property(List.of("discount")),
                new NumberLiteral(new BigDecimal("15"))
        );

        var rule = new Rule(
                "Premium discount",
                condition,
                assignment
        );

        var program = new Program(List.of(rule));

        System.out.println("AST:");
        System.out.println(program);

        assertEquals(1, program.rules().size());

        var actualRule = program.rules().getFirst();

        assertEquals("Premium discount", actualRule.name());

        var expression = assertInstanceOf(
                And.class,
                actualRule.condition()
        );

        assertEquals(List.of(age, premium), expression.operands());
        assertEquals(assignment, actualRule.assignment());
    }

    @Test
    void shouldProtectPropertyPathFromModification() {
        var path = new ArrayList<>(
                List.of("customer", "address", "city")
        );

        var property = new Property(path);

        path.clear();

        assertEquals(
                List.of("customer", "address", "city"),
                property.path()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> property.path().clear()
        );
    }
}
