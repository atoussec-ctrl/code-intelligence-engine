package com.rag.rag.application.rag;

import java.util.Objects;
import java.util.UUID;

public record RetrievedContext(
        UUID workspaceId,
        UUID chunkId,
        UUID documentId,
        String sourceTitle,
        String content,
        double score
) {
    public RetrievedContext {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
        chunkId = Objects.requireNonNull(chunkId, "chunkId is required");
        documentId = Objects.requireNonNull(documentId, "documentId is required");
        sourceTitle = requireText(sourceTitle, "sourceTitle is required");
        content = requireText(content, "content is required");
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}