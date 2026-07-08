package com.rag.rag.application.rag;

import com.rag.rag.application.port.out.VectorSearchPort;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RetrieveContextUseCase {
    private final VectorSearchPort vectorSearch;

    public RetrieveContextUseCase(VectorSearchPort vectorSearch) {
        this.vectorSearch = Objects.requireNonNull(vectorSearch, "vectorSearch is required");
    }

    public List<RetrievedContext> execute(RetrievalQuery query) {
        Objects.requireNonNull(query, "query is required");

        return vectorSearch.search(query).stream()
                .filter(context -> query.workspaceId().equals(context.workspaceId()))
                .sorted(Comparator.comparingDouble(RetrievedContext::score).reversed())
                .limit(query.topK())
                .toList();
    }
}