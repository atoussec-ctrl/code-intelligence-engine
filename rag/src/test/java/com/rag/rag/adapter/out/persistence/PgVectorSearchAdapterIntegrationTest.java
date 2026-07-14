package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.rag.rag.application.rag.RetrievedContext;
import com.rag.rag.application.rag.VectorSearchQuery;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PgVectorSearchAdapterIntegrationTest extends PgVectorIntegrationTestSupport {

    private PgVectorSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PgVectorSearchAdapter(jdbcTemplate);
    }

    @Test
    void searchesPgvectorWithoutLeakingResultsAcrossWorkspaces() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();

        insertKnowledge(workspaceA, "Architecture Notes", "Ports isolate adapters.", "[1,0,0]");
        insertKnowledge(workspaceA, "Messaging Notes", "Queues handle async ingestion.", "[0,1,0]");
        insertKnowledge(workspaceB, "Private Tenant Notes", "This tenant must not leak.", "[1,0,0]");

        List<RetrievedContext> results = adapter.search(new VectorSearchQuery(
                workspaceA,
                EmbeddingVector.of(List.of(1.0, 0.0, 0.0), "test-model"),
                5));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> workspaceA.equals(result.workspaceId()));
        assertThat(results)
                .extracting(RetrievedContext::sourceTitle)
                .containsExactly("Architecture Notes", "Messaging Notes");
        assertThat(results.getFirst().score()).isCloseTo(1.0, offset(0.0001));
        assertThat(results.getFirst().score()).isGreaterThan(results.getLast().score());
    }

    private void insertKnowledge(UUID workspaceId, String title, String content, String embedding) {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO documents (id, workspace_id, title, source_type, checksum, status, metadata)
                VALUES (?, ?, ?, 'TEXT', ?, 'READY', '{}'::jsonb)
                """,
                documentId,
                workspaceId,
                title,
                "checksum-" + documentId);
        jdbcTemplate.update(
                """
                INSERT INTO chunks (id, workspace_id, document_id, chunk_index, content, token_count, metadata)
                VALUES (?, ?, ?, 0, ?, 4, '{}'::jsonb)
                """,
                chunkId,
                workspaceId,
                documentId,
                content);
        jdbcTemplate.update(
                """
                INSERT INTO chunk_embeddings (chunk_id, workspace_id, document_id, model, embedding)
                VALUES (?, ?, ?, 'test-model', ?::vector)
                """,
                chunkId,
                workspaceId,
                documentId,
                embedding);
    }
}
