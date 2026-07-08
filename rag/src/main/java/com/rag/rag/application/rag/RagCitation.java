package com.rag.rag.application.rag;

import java.util.Objects;
import java.util.UUID;

public record RagCitation(UUID chunkId, UUID documentId, String sourceTitle) {
    public RagCitation {
        chunkId = Objects.requireNonNull(chunkId, "chunkId is required");
        documentId = Objects.requireNonNull(documentId, "documentId is required");
        if (sourceTitle == null || sourceTitle.isBlank()) {
            throw new IllegalArgumentException("sourceTitle is required");
        }
        sourceTitle = sourceTitle.trim();
    }
}