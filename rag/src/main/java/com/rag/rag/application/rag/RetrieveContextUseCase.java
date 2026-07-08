package com.rag.rag.application.rag;

import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.port.out.VectorSearchPort;
import java.util.List;
import java.util.Objects;

public class RetrieveContextUseCase {
    private final EmbeddingGeneratorPort embeddings;
    private final VectorSearchPort vectorSearch;

    public RetrieveContextUseCase(EmbeddingGeneratorPort embeddings, VectorSearchPort vectorSearch) {
        this.embeddings = Objects.requireNonNull(embeddings, "embeddings is required");
        this.vectorSearch = Objects.requireNonNull(vectorSearch, "vectorSearch is required");
    }

    public List<RetrievedContext> execute(RetrievalQuery query) {
        Objects.requireNonNull(query, "query is required");
        var queryEmbeddings = embeddings.generateBatch(List.of(query.query()));
        if (queryEmbeddings.size() != 1) {
            throw new IllegalStateException("query embedding must contain exactly one vector");
        }

        var vectorQuery = new VectorSearchQuery(query.workspaceId(), queryEmbeddings.getFirst(), query.topK());
        return vectorSearch.search(vectorQuery).stream()
                .filter(context -> query.workspaceId().equals(context.workspaceId()))
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(query.topK())
                .toList();
    }
}
