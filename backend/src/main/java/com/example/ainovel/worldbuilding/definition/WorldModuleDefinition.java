package com.example.ainovel.worldbuilding.definition;

import java.util.List;

public record WorldModuleDefinition(
        String key,
        String label,
        List<FieldDefinition> fields,
        int order
) {
    public record FieldDefinition(
            String key,
            String label,
            boolean required,
            int minLength,
            int maxLength
    ) {
    }
}
