package com.rag.rag.adapter.out.persistence;

import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.stream.Collectors;

final class PgVectorLiteral {

    private PgVectorLiteral() {
    }

    static String from(EmbeddingVector embedding) {
        return embedding.values().stream()
                .map(PgVectorLiteral::formatVectorValue)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String formatVectorValue(Double value) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException("embedding values must be finite");
        }
        return value.toString();
    }
}
