package com.example.ainovel.prompt;

public class PromptTemplateException extends RuntimeException {

    public PromptTemplateException(String message) {
        super(message);
    }

    public PromptTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
