package com.example.ainovel.prompt;

public enum PromptType {
    STORY_CREATION("storyCreation"),
    OUTLINE_CHAPTER("outlineChapter"),
    MANUSCRIPT_SECTION("manuscriptSection"),
    REFINE_WITH_INSTRUCTION("refine.withInstruction"),
    REFINE_WITHOUT_INSTRUCTION("refine.withoutInstruction");

    private final String key;

    PromptType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static PromptType fromKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim();
        for (PromptType type : values()) {
            if (type.key.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
