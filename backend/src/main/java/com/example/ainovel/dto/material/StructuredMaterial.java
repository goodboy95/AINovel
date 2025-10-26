package com.example.ainovel.dto.material;

import java.util.List;

import lombok.Data;

/**
 * 对应 material_base_instruction.md 中的结构化 JSON Schema。
 */
@Data
public class StructuredMaterial {

    private String type;
    private String title;
    private List<String> aliases;
    private String summary;
    private List<String> tags;
    private CharacterSection character;
    private PlaceSection place;
    private EventSection event;

    @Data
    public static class CharacterSection {
        private String name;
        private String age;
        private List<String> traits;
        private List<CharacterRelation> relations;
        private String backstory;
    }

    @Data
    public static class CharacterRelation {
        private String to;
        private String relation;
    }

    @Data
    public static class PlaceSection {
        private String name;
        private String era;
        private String geo;
    }

    @Data
    public static class EventSection {
        private String time;
        private List<String> participants;
        private String outcome;
    }
}

