package com.example.ainovel.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateEngineTest {

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        templateEngine = new TemplateEngine(new ObjectMapper());
    }

    @Test
    void rendersSimpleProperty() {
        Map<String, Object> context = Map.of(
                "user",
                Map.of("name", "Alice")
        );
        String result = templateEngine.render("Hello ${user.name}!", context);
        assertEquals("Hello Alice!", result);
    }

    @Test
    void rendersListWithStarAndJoin() {
        Map<String, Object> context = Map.of(
                "items",
                List.of(
                        Map.of("value", "Alpha"),
                        Map.of("value", "Beta"),
                        Map.of("value", "Gamma")
                )
        );
        String result = templateEngine.render("Values: ${items[*].value|join(\", \")}", context);
        assertEquals("Values: Alpha, Beta, Gamma", result);
    }

    @Test
    void appliesDefaultFunctionForMissingValue() {
        String result = templateEngine.render("Value: ${missing|default(\"fallback\")}", Map.of());
        assertEquals("Value: fallback", result);
    }

    @Test
    void rendersEscapedExpression() {
        String result = templateEngine.render("Show literal: $${value}", Map.of("value", "ignored"));
        assertEquals("Show literal: ${value}", result);
    }

    @Test
    void serializesJsonFunction() {
        Map<String, Object> context = Map.of("payload", Map.of("key", "value"));
        String result = templateEngine.render("${payload|json}", context);
        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    void throwsForUnknownFunction() {
        assertThrows(PromptTemplateException.class,
                () -> templateEngine.render("${value|unknown()}", Map.of("value", "test")));
    }
}
