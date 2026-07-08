package com.rag.rag.application.rag;

public record RagPrompt(
        String systemInstructions,
        String userQuestion,
        String retrievedContext,
        String citationRules,
        String responseFormat
) {
    public RagPrompt {
        systemInstructions = requireText(systemInstructions, "systemInstructions is required");
        userQuestion = requireText(userQuestion, "userQuestion is required");
        retrievedContext = requireText(retrievedContext, "retrievedContext is required");
        citationRules = requireText(citationRules, "citationRules is required");
        responseFormat = requireText(responseFormat, "responseFormat is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}