package com.example.ainovel.model.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PromptSettingsAttributeConverter implements AttributeConverter<PromptSettings, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Override
    public String convertToDatabaseColumn(PromptSettings attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize prompt settings", e);
        }
    }

    @Override
    public PromptSettings convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, PromptSettings.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize prompt settings", e);
        }
    }
}
