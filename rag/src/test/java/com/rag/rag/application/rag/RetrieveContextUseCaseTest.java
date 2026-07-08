package com.rag.rag.application.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.rag.rag.application.port.out.VectorSearchPort;

class RetrieveContextUseCaseTest {
    @Test
    void shouldNeverReturnChunksFromAnotherWorkspace() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        FakeVectorSearchPort vectorSearch = new FakeVectorSearchPort(List.of(
                context(workspaceA, 0.91),
                context(workspaceB, 0.99),
                context(workspaceA, 0.87)
        ));
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(vectorSearch);

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
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(vectorSearch);

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
    void shouldReturnImmutableResultList() {
        UUID workspaceId = UUID.randomUUID();
        RetrieveContextUseCase useCase = new RetrieveContextUseCase(new FakeVectorSearchPort(List.of(context(workspaceId, 0.91))));

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

    private static class FakeVectorSearchPort implements VectorSearchPort {
        private final List<RetrievedContext> results;
        private RetrievalQuery lastQuery;

        FakeVectorSearchPort(List<RetrievedContext> results) {
            this.results = new ArrayList<>(results);
        }

        @Override
        public List<RetrievedContext> search(RetrievalQuery query) {
            lastQuery = query;
            return results;
        }

        RetrievalQuery lastQuery() {
            return lastQuery;
        }
    }
}