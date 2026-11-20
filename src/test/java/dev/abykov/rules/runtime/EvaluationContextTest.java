package dev.abykov.rules.runtime;

import dev.abykov.rules.ast.Property;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationContextTest {

    private final EvaluationContext context =
            new EvaluationContext(Map.of(
                    "discount", 15,
                    "customer", Map.of(
                            "age", 25,
                            "premium", true,
                            "address", Map.of(
                                    "city", "Moscow"
                            )
                    ),
                    "order", Map.of(
                            "total", 15000
                    )
            ));

    @Test
    void shouldResolveSimpleProperty() {
        assertResolved(15, "discount");
    }

    @Test
    void shouldResolveNestedProperty() {
        assertResolved(25, "customer", "age");
    }

    @Test
    void shouldResolveDeeplyNestedProperty() {
        assertResolved(
                "Moscow",
                "customer", "address", "city"
        );
    }

    @Test
    void shouldResolveBooleanProperty() {
        assertResolved(true, "customer", "premium");
    }

    @Test
    void shouldRejectUnknownRootProperty() {
        assertUnresolved("unknown");
    }

    @Test
    void shouldRejectUnknownNestedProperty() {
        assertUnresolved("customer", "phone");
    }

    @Test
    void shouldRejectTraversalThroughScalarValue() {
        assertUnresolved("customer", "age", "value");
    }

    @Test
    void shouldResolveNullValue() {
        var data = new HashMap<String, Object>();
        data.put("value", null);

        var nullableContext = new EvaluationContext(data);

        assertNull(nullableContext.resolve(
                new Property(List.of("value"))
        ));
    }

    @Test
    void shouldRejectTraversalThroughNullValue() {
        var data = new HashMap<String, Object>();
        data.put("customer", null);

        var nullableContext = new EvaluationContext(data);

        assertThrows(
                EvaluationException.class,
                () -> nullableContext.resolve(
                        new Property(List.of("customer", "age"))
                )
        );
    }

    @Test
    void shouldProtectContextFromExternalChanges() {
        var source = new HashMap<String, Object>();
        source.put("age", 25);

        var context = new EvaluationContext(source);

        source.put("age", 17);

        assertEquals(
                25,
                context.resolve(new Property(List.of("age")))
        );
    }

    @Test
    void shouldProtectNestedContextFromExternalChanges() {
        var customer = new HashMap<String, Object>();
        customer.put("age", 25);

        var context = new EvaluationContext(
                Map.of("customer", customer)
        );

        customer.put("age", 17);

        assertEquals(
                25,
                context.resolve(
                        new Property(List.of("customer", "age"))
                )
        );
    }

    @Test
    void shouldProtectNestedListFromExternalChanges() {
        var items = new ArrayList<>(List.of("first"));

        var context = new EvaluationContext(
                Map.of("items", items)
        );

        items.add("second");

        assertEquals(
                List.of("first"),
                context.resolve(new Property(List.of("items")))
        );
    }

    @Test
    void shouldPreserveNullValuesInSnapshot() {
        var source = new HashMap<String, Object>();
        source.put("value", null);

        var context = new EvaluationContext(source);

        assertNull(
                context.resolve(new Property(List.of("value")))
        );
    }

    private void assertResolved(
            Object expected,
            String... path) {

        var property = new Property(List.of(path));

        var actual = context.resolve(property);

        assertEquals(expected, actual);

        System.out.printf(
                "%s -> %s%n",
                String.join(".", path),
                actual
        );
    }

    private void assertUnresolved(String... path) {
        var property = new Property(List.of(path));

        var exception = assertThrows(
                EvaluationException.class,
                () -> context.resolve(property)
        );

        assertTrue(exception.getMessage().contains(
                String.join(".", path)
        ));

        System.out.println(exception.getMessage());
    }
}
