package com.rag.rag.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.port.out.VectorSearchPort;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrieveContextUseCaseTest {
    @Test
    void shouldGenerateQueryEmbeddingAndSearchByWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        EmbeddingVector queryEmbedding = EmbeddingVector.of(List.of(0.1, 0.2, 0.3), "test-model");
        FakeEmbeddingGeneratorPort embeddings = new FakeEmbeddingGeneratorPort(List.of(queryEmbedding));
        FakeVectorSearchPort vectorSearch = new FakeVectorSearchPort(List.of(context(workspaceId, 0.91)));
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(embeddings, vectorSearch);

        List<RetrievedContext> results = useCase.execute(new RetrievalQuery(workspaceId, "architecture", 5));

        assertThat(results).hasSize(1);
        assertThat(embeddings.lastTexts()).containsExactly("architecture");
        assertThat(vectorSearch.lastQuery().workspaceId()).isEqualTo(workspaceId);
        assertThat(vectorSearch.lastQuery().embedding()).isEqualTo(queryEmbedding);
        assertThat(vectorSearch.lastQuery().topK()).isEqualTo(5);
    }

    @Test
    void shouldNeverReturnChunksFromAnotherWorkspace() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        FakeVectorSearchPort vectorSearch = new FakeVectorSearchPort(List.of(
                context(workspaceA, 0.91),
                context(workspaceB, 0.99),
                context(workspaceA, 0.87)
        ));
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(defaultEmbeddings(), vectorSearch);

        List<RetrievedContext> results = useCase.execute(new RetrievalQuery(workspaceA, "architecture", 5));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> result.workspaceId().equals(workspaceA));
        assertThat(vectorSearch.lastQuery().workspaceId()).isEqualTo(workspaceA);
    }

    @Test
    void shouldReturnHighestScoredContextsWithinTopK() {
        UUID workspaceId = UUID.randomUUID();
        FakeVectorSearchPort vectorSearch = new FakeVectorSearchPort(List.of(
                context(workspaceId, 0.20),
                context(workspaceId, 0.95),
                context(workspaceId, 0.70)
        ));
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(defaultEmbeddings(), vectorSearch);

        List<RetrievedContext> results = useCase.execute(new RetrievalQuery(workspaceId, "architecture", 2));

        assertThat(results).extracting((RetrievedContext result) -> result.score()).containsExactly(0.95, 0.70);
        assertThat(vectorSearch.lastQuery().topK()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidQuery() {
        UUID workspaceId = UUID.randomUUID();

        assertThatThrownBy(() -> new RetrievalQuery(workspaceId, " ", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query is required");
        assertThatThrownBy(() -> new RetrievalQuery(workspaceId, "architecture", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topK must be positive");
    }

    @Test
    void shouldRejectUnexpectedQueryEmbeddingCount() {
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(
                new FakeEmbeddingGeneratorPort(List.of()),
                new FakeVectorSearchPort(List.of()));

        assertThatThrownBy(() -> useCase.execute(new RetrievalQuery(UUID.randomUUID(), "architecture", 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("query embedding must contain exactly one vector");
    }

    @Test
    void shouldReturnImmutableResultList() {
        UUID workspaceId = UUID.randomUUID();
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(defaultEmbeddings(), new FakeVectorSearchPort(List.of(context(workspaceId, 0.91))));

        List<RetrievedContext> results = useCase.execute(new RetrievalQuery(workspaceId, "architecture", 5));

        assertThatThrownBy(() -> results.add(context(workspaceId, 0.80)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static RetrievedContext context(UUID workspaceId, double score) {
        return new RetrievedContext(
                workspaceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Architecture Notes",
                "Ports and adapters separate policy from infrastructure.",
                score
        );
    }

    private static FakeEmbeddingGeneratorPort defaultEmbeddings() {
        return new FakeEmbeddingGeneratorPort(List.of(EmbeddingVector.of(List.of(0.1, 0.2, 0.3), "test-model")));
    }

    private static class FakeEmbeddingGeneratorPort implements EmbeddingGeneratorPort {
        private final List<EmbeddingVector> embeddings;
        private List<String> lastTexts = List.of();

        FakeEmbeddingGeneratorPort(List<EmbeddingVector> embeddings) {
            this.embeddings = List.copyOf(embeddings);
        }

        @Override
        public List<EmbeddingVector> generateBatch(List<String> texts) {
            lastTexts = List.copyOf(texts);
            return embeddings;
        }

        List<String> lastTexts() {
            return lastTexts;
        }
    }

    private static class FakeVectorSearchPort implements VectorSearchPort {
        private final List<RetrievedContext> results;
        private VectorSearchQuery lastQuery;

        FakeVectorSearchPort(List<RetrievedContext> results) {
            this.results = new ArrayList<>(results);
        }

        @Override
        public List<RetrievedContext> search(VectorSearchQuery query) {
            lastQuery = query;
            return results;
        }

        VectorSearchQuery lastQuery() {
            return lastQuery;
        }
    }
}
