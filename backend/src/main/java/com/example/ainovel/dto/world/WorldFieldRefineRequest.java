package com.example.ainovel.dto.world;

public class WorldFieldRefineRequest {

    private String text;
    private String instruction;

    public String getText() {
        return text;
    }

    public WorldFieldRefineRequest setText(String text) {
        this.text = text;
        return this;
    }

    public String getInstruction() {
        return instruction;
    }

    public WorldFieldRefineRequest setInstruction(String instruction) {
        this.instruction = instruction;
        return this;
    }
}
