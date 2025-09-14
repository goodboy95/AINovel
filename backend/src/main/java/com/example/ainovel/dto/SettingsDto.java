package com.example.ainovel.dto;

public class SettingsDto {
    private String baseUrl;
    private String modelName;
    /**
     * Deprecated for response: do not populate from server responses.
     * Kept for update requests only.
     */
    private String apiKey;
    private String customPrompt;

    /**
     * Indicates whether an API key has been configured for the user.
     * The server MUST NOT return the decrypted or raw API key; only this flag.
     */
    private boolean apiKeyIsSet;

    // Getters and Setters

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    public boolean isApiKeyIsSet() {
        return apiKeyIsSet;
    }

    public void setApiKeyIsSet(boolean apiKeyIsSet) {
        this.apiKeyIsSet = apiKeyIsSet;
    }
}
