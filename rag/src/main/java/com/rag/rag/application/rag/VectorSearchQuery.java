package com.rag.rag.application.rag;

import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.Objects;
import java.util.UUID;

public record VectorSearchQuery(UUID workspaceId, EmbeddingVector embedding, int topK) {
    public VectorSearchQuery {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
        embedding = Objects.requireNonNull(embedding, "embedding is required");
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
    }
}
