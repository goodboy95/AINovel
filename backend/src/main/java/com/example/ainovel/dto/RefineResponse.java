package com.example.ainovel.dto;

public class RefineResponse {
    private final String refinedText;

    public RefineResponse(String refinedText) {
        this.refinedText = refinedText;
    }

    public String getRefinedText() {
        return refinedText;
    }
}
