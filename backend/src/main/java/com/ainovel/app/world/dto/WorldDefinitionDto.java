package com.ainovel.app.world.dto;

import java.util.List;

public record WorldDefinitionDto(String key, String label, String description, List<Field> fields) {
    public record Field(String key, String label, String type, String placeholder) {}
}
