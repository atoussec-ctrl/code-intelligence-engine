package com.rag.rag.application.rag;

import java.util.List;
import java.util.Objects;

public record RagAnswer(String answer, List<RagCitation> citations) {
    public RagAnswer {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("answer is required");
        }
        answer = answer.trim();
        citations = List.copyOf(Objects.requireNonNull(citations, "citations are required"));
    }
}