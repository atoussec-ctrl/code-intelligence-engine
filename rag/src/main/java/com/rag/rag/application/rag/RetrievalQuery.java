package com.rag.rag.application.rag;

import java.util.Objects;
import java.util.UUID;

public record RetrievalQuery(UUID workspaceId, String query, int topK) {
    public RetrievalQuery {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        query = query.trim();
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
    }
}