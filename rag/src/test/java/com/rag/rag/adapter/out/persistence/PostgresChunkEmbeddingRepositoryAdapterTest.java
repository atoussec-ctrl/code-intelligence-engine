package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rag.rag.domain.document.Chunk;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresChunkEmbeddingRepositoryAdapterTest {

    @Test
    void savesChunksAndEmbeddingsWithPreparedSql() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        PostgresChunkEmbeddingRepositoryAdapter adapter = new PostgresChunkEmbeddingRepositoryAdapter(jdbcTemplate);
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Chunk chunk = Chunk.create(
                workspaceId,
                documentId,
                0,
                "Ports isolate infrastructure.",
                3,
                Map.of("section", "architecture"));
        ChunkEmbedding chunkEmbedding = ChunkEmbedding.of(
                chunk,
                EmbeddingVector.of(List.of(1.0, 0.0, 0.5), "test-model"));

        adapter.saveAll(workspaceId, documentId, List.of(chunkEmbedding));

        assertThat(jdbcTemplate.updates()).hasSize(3);
        RecordedUpdate delete = jdbcTemplate.updates().get(0);
        assertThat(delete.sql()).contains("DELETE FROM chunks WHERE workspace_id = ? AND document_id = ?");
        assertThat(delete.args()).containsExactly(workspaceId, documentId);

        RecordedUpdate insertChunk = jdbcTemplate.updates().get(1);
        assertThat(insertChunk.sql()).contains("INSERT INTO chunks").contains("?::jsonb");
        assertThat(insertChunk.args()[0]).isEqualTo(chunk.id());
        assertThat(insertChunk.args()[1]).isEqualTo(workspaceId);
        assertThat(insertChunk.args()[2]).isEqualTo(documentId);
        assertThat(insertChunk.args()[3]).isEqualTo(0);
        assertThat(insertChunk.args()[4]).isEqualTo("Ports isolate infrastructure.");
        assertThat(insertChunk.args()[5]).isEqualTo(3);
        assertThat(JsonbMetadata.read((String) insertChunk.args()[6]))
                .containsEntry("section", "architecture");

        RecordedUpdate insertEmbedding = jdbcTemplate.updates().get(2);
        assertThat(insertEmbedding.sql()).contains("INSERT INTO chunk_embeddings").contains("?::vector");
        assertThat(insertEmbedding.args()).containsExactly(
                chunk.id(),
                workspaceId,
                documentId,
                "test-model",
                "[1.0,0.0,0.5]");
    }

    @Test
    void replacesExistingRowsEvenWhenNewChunkListIsEmpty() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        PostgresChunkEmbeddingRepositoryAdapter adapter = new PostgresChunkEmbeddingRepositoryAdapter(jdbcTemplate);
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        adapter.saveAll(workspaceId, documentId, List.of());

        assertThat(jdbcTemplate.updates()).hasSize(1);
        assertThat(jdbcTemplate.updates().getFirst().sql()).contains("DELETE FROM chunks");
        assertThat(jdbcTemplate.updates().getFirst().args()).containsExactly(workspaceId, documentId);
    }

    @Test
    void rejectsChunkOutsideRepositoryScopeBeforeMutatingDatabase() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        PostgresChunkEmbeddingRepositoryAdapter adapter = new PostgresChunkEmbeddingRepositoryAdapter(jdbcTemplate);
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Chunk wrongWorkspaceChunk = Chunk.create(
                UUID.randomUUID(),
                documentId,
                0,
                "Wrong workspace.",
                2,
                Map.of());

        assertThatThrownBy(() -> adapter.saveAll(
                workspaceId,
                documentId,
                List.of(ChunkEmbedding.of(wrongWorkspaceChunk, EmbeddingVector.of(List.of(1.0), "test-model")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chunk scope must match repository scope");
        assertThat(jdbcTemplate.updates()).isEmpty();
    }

    @Test
    void requiresDependenciesAndArguments() {
        PostgresChunkEmbeddingRepositoryAdapter adapter =
                new PostgresChunkEmbeddingRepositoryAdapter(new RecordingJdbcTemplate());

        assertThatThrownBy(() -> new PostgresChunkEmbeddingRepositoryAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate is required");
        assertThatThrownBy(() -> adapter.saveAll(null, UUID.randomUUID(), List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("workspaceId is required");
        assertThatThrownBy(() -> adapter.saveAll(UUID.randomUUID(), null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("documentId is required");
        assertThatThrownBy(() -> adapter.saveAll(UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("chunks are required");
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<RecordedUpdate> updates = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {
            updates.add(new RecordedUpdate(sql, args.clone()));
            return 1;
        }

        private List<RecordedUpdate> updates() {
            return List.copyOf(updates);
        }
    }

    private record RecordedUpdate(String sql, Object[] args) {
    }
}
