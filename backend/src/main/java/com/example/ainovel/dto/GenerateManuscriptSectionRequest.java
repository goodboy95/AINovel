package com.example.ainovel.dto;

/**
 * Request payload for manuscript section generation.
 */
public class GenerateManuscriptSectionRequest {

    /**
     * Optional world identifier selected by the user when requesting generation.
     */
    private Long worldId;

    public Long getWorldId() {
        return worldId;
    }

    public void setWorldId(Long worldId) {
        this.worldId = worldId;
    }
}
