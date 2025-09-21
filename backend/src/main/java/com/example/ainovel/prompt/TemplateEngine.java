package com.example.ainovel.prompt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TemplateEngine {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^{}]+)}");
    private static final String ESCAPED_SEQUENCE = "__PROMPT_TEMPLATE_ESCAPED__";

    private final ObjectMapper objectMapper;

    public TemplateEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(String template, Map<String, Object> context) {
        if (template == null) {
            return "";
        }
        Map<String, Object> safeContext = context == null ? Collections.emptyMap() : context;
        String prepared = template.replace("$${", ESCAPED_SEQUENCE + "{");
        Matcher matcher = EXPRESSION_PATTERN.matcher(prepared);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            ValueWrapper value = evaluateExpression(expression, safeContext);
            String replacement = value.toFinalString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace(ESCAPED_SEQUENCE + "{", "${");
    }

    private ValueWrapper evaluateExpression(String expression, Map<String, Object> context) {
        if (!StringUtils.hasText(expression)) {
            return ValueWrapper.empty();
        }
        List<String> segments = splitPipeline(expression);
        if (segments.isEmpty()) {
            return ValueWrapper.empty();
        }
        ValueWrapper value = resolvePath(segments.get(0).trim(), context);
        for (int i = 1; i < segments.size(); i++) {
            String segment = segments.get(i).trim();
            if (segment.isEmpty()) {
                continue;
            }
            value = applyFunction(value, segment);
        }
        return value;
    }

    private ValueWrapper resolvePath(String pathExpression, Map<String, Object> context) {
        if (!StringUtils.hasText(pathExpression)) {
            return ValueWrapper.empty();
        }
        List<PathToken> tokens = parsePath(pathExpression);
        List<Object> current = new ArrayList<>();
        current.add(context);
        boolean starEncountered = false;
        for (PathToken token : tokens) {
            List<Object> next = new ArrayList<>();
            for (Object value : current) {
                Object propertyValue = extractPropertyValue(value, token.name());
                List<Object> intermediate = new ArrayList<>();
                intermediate.add(propertyValue);
                for (IndexToken index : token.indices()) {
                    if (index.star()) {
                        starEncountered = true;
                        List<Object> expanded = new ArrayList<>();
                        for (Object item : intermediate) {
                            if (item == null) {
                                continue;
                            }
                            if (item instanceof Iterable<?> iterable) {
                                for (Object element : iterable) {
                                    if (element != null) {
                                        expanded.add(element);
                                    }
                                }
                            } else if (item.getClass().isArray()) {
                                int length = Array.getLength(item);
                                for (int i = 0; i < length; i++) {
                                    Object element = Array.get(item, i);
                                    if (element != null) {
                                        expanded.add(element);
                                    }
                                }
                            }
                        }
                        intermediate = expanded;
                    } else {
                        int idx = index.index();
                        List<Object> selected = new ArrayList<>();
                        for (Object item : intermediate) {
                            if (item == null) {
                                selected.add(null);
                            } else if (item instanceof List<?> list) {
                                selected.add(idx >= 0 && idx < list.size() ? list.get(idx) : null);
                            } else if (item.getClass().isArray()) {
                                int length = Array.getLength(item);
                                selected.add(idx >= 0 && idx < length ? Array.get(item, idx) : null);
                            } else {
                                selected.add(null);
                            }
                        }
                        intermediate = selected;
                    }
                }
                next.addAll(intermediate);
            }
            current = next;
        }
        if (current.isEmpty()) {
            return ValueWrapper.empty(starEncountered);
        }
        if (current.size() == 1 && !starEncountered) {
            return ValueWrapper.of(current.get(0));
        }
        return ValueWrapper.ofList(current, starEncountered ? "、" : ", ");
    }

    private Object extractPropertyValue(Object value, String propertyName) {
        if (value == null) {
            return null;
        }
        if (!StringUtils.hasText(propertyName)) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            return map.get(propertyName);
        }
        if (value instanceof List<?> list && isNumeric(propertyName)) {
            int index = Integer.parseInt(propertyName);
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }
        if (value.getClass().isArray() && isNumeric(propertyName)) {
            int index = Integer.parseInt(propertyName);
            int length = Array.getLength(value);
            return index >= 0 && index < length ? Array.get(value, index) : null;
        }
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(value);
            if (wrapper.isReadableProperty(propertyName)) {
                return wrapper.getPropertyValue(propertyName);
            }
        } catch (Exception ignored) {
            // Ignore access issues and fall back to null
        }
        return null;
    }

    private ValueWrapper applyFunction(ValueWrapper current, String functionSegment) {
        int parenIndex = functionSegment.indexOf('(');
        String name;
        String argsSection;
        if (parenIndex < 0) {
            name = functionSegment.trim();
            argsSection = "";
        } else {
            name = functionSegment.substring(0, parenIndex).trim();
            int endIndex = functionSegment.lastIndexOf(')');
            if (endIndex < parenIndex) {
                throw new PromptTemplateException("Malformed function expression: " + functionSegment);
            }
            argsSection = functionSegment.substring(parenIndex + 1, endIndex);
        }
        List<String> args = parseArguments(argsSection);
        return switch (name) {
            case "default" -> applyDefault(current, args);
            case "join" -> applyJoin(current, args);
            case "upper" -> ValueWrapper.of(current.asString().toUpperCase(Locale.ROOT));
            case "lower" -> ValueWrapper.of(current.asString().toLowerCase(Locale.ROOT));
            case "trim" -> ValueWrapper.of(current.asString().trim());
            case "json" -> applyJson(current);
            case "" -> current;
            default -> throw new PromptTemplateException("Unknown function: " + name);
        };
    }

    private ValueWrapper applyDefault(ValueWrapper current, List<String> args) {
        if (args.isEmpty()) {
            throw new PromptTemplateException("default() requires an argument");
        }
        String fallback = args.get(0);
        String value = current.asString();
        if (!StringUtils.hasText(value)) {
            return ValueWrapper.of(fallback);
        }
        return ValueWrapper.of(value);
    }

    private ValueWrapper applyJoin(ValueWrapper current, List<String> args) {
        List<Object> list = current.asList();
        String separator = args.isEmpty() ? current.defaultSeparator() : args.get(0);
        String joined = list.stream()
                .map(this::stringify)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(separator));
        return ValueWrapper.of(joined);
    }

    private ValueWrapper applyJson(ValueWrapper current) {
        try {
            return ValueWrapper.of(objectMapper.writeValueAsString(current.rawValue()));
        } catch (JsonProcessingException e) {
            throw new PromptTemplateException("Failed to serialize value as JSON", e);
        }
    }

    private List<String> splitPipeline(String expression) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';
        int parentheses = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if ((ch == '\'' || ch == '"')) {
                if (inQuotes && ch == quoteChar) {
                    inQuotes = false;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = ch;
                }
            } else if (!inQuotes) {
                if (ch == '(') {
                    parentheses++;
                } else if (ch == ')' && parentheses > 0) {
                    parentheses--;
                } else if (ch == '|' && parentheses == 0) {
                    segments.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }
        return segments;
    }

    private List<PathToken> parsePath(String expression) {
        List<PathToken> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '.' && bracketDepth == 0) {
                tokens.add(parseSegment(current.toString()));
                current.setLength(0);
                continue;
            }
            if (ch == '[') {
                bracketDepth++;
            } else if (ch == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(parseSegment(current.toString()));
        }
        if (tokens.isEmpty()) {
            throw new PromptTemplateException("Invalid path expression: " + expression);
        }
        return tokens;
    }

    private PathToken parseSegment(String segment) {
        String trimmed = segment.trim();
        if (trimmed.isEmpty()) {
            return new PathToken("", List.of());
        }
        List<IndexToken> indices = new ArrayList<>();
        String name = trimmed;
        int bracketIndex = trimmed.indexOf('[');
        if (bracketIndex >= 0) {
            name = trimmed.substring(0, bracketIndex).trim();
            int cursor = bracketIndex;
            while (cursor >= 0 && cursor < trimmed.length()) {
                int close = trimmed.indexOf(']', cursor);
                if (close < 0) {
                    throw new PromptTemplateException("Unclosed index segment in: " + segment);
                }
                String inside = trimmed.substring(cursor + 1, close).trim();
                if ("*".equals(inside)) {
                    indices.add(IndexToken.wildcard());
                } else if (!inside.isEmpty()) {
                    if (!isNumeric(inside)) {
                        throw new PromptTemplateException("Invalid index: " + inside);
                    }
                    indices.add(IndexToken.of(Integer.parseInt(inside)));
                } else {
                    throw new PromptTemplateException("Index cannot be empty: " + segment);
                }
                cursor = trimmed.indexOf('[', close);
            }
        }
        return new PathToken(name, indices);
    }

    private List<String> parseArguments(String argsSection) {
        if (!StringUtils.hasText(argsSection)) {
            return Collections.emptyList();
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';
        int parentheses = 0;
        for (int i = 0; i < argsSection.length(); i++) {
            char ch = argsSection.charAt(i);
            if ((ch == '\'' || ch == '"')) {
                if (inQuotes && ch == quoteChar) {
                    inQuotes = false;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = ch;
                }
            } else if (!inQuotes) {
                if (ch == '(') {
                    parentheses++;
                } else if (ch == ')' && parentheses > 0) {
                    parentheses--;
                } else if (ch == ',' && parentheses == 0) {
                    args.add(stripQuotes(current.toString().trim()));
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            args.add(stripQuotes(current.toString().trim()));
        }
        return args;
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean isNumeric(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::stringify).collect(java.util.stream.Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private record PathToken(String name, List<IndexToken> indices) {
    }

    private record IndexToken(boolean star, int index) {
        static IndexToken wildcard() {
            return new IndexToken(true, -1);
        }

        static IndexToken of(int index) {
            return new IndexToken(false, index);
        }
    }

    private static final class ValueWrapper {
        private final Object value;
        private final boolean listLike;
        private final String defaultSeparator;

        private ValueWrapper(Object value, boolean listLike, String defaultSeparator) {
            this.value = value;
            this.listLike = listLike;
            this.defaultSeparator = defaultSeparator;
        }

        static ValueWrapper of(Object value) {
            return new ValueWrapper(value, false, "、");
        }

        static ValueWrapper ofList(List<Object> values, String defaultSeparator) {
            return new ValueWrapper(values, true, defaultSeparator);
        }

        static ValueWrapper empty() {
            return new ValueWrapper("", false, "、");
        }

        static ValueWrapper empty(boolean asList) {
            if (asList) {
                return new ValueWrapper(new ArrayList<>(), true, "、");
            }
            return empty();
        }

        Object rawValue() {
            return value;
        }

        String defaultSeparator() {
            return defaultSeparator;
        }

        List<Object> asList() {
            if (value == null) {
                return new ArrayList<>();
            }
            if (listLike && value instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            if (value instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            if (value instanceof Collection<?> collection) {
                return new ArrayList<>(collection);
            }
            if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> result = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    result.add(Array.get(value, i));
                }
                return result;
            }
            List<Object> single = new ArrayList<>();
            single.add(value);
            return single;
        }

        String asString() {
            return toFinalString();
        }

        String toFinalString() {
            if (value == null) {
                return "";
            }
            if (listLike) {
                return asList().stream()
                        .map(item -> item == null ? "" : item.toString())
                        .collect(java.util.stream.Collectors.joining(defaultSeparator));
            }
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .map(item -> item == null ? "" : item.toString())
                        .collect(java.util.stream.Collectors.joining(defaultSeparator));
            }
            return String.valueOf(value);
        }
    }
}
