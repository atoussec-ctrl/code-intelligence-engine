package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rag.rag.domain.document.Chunk;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresPersistenceAdaptersIntegrationTest extends PgVectorIntegrationTestSupport {

    private PostgresDocumentRepositoryAdapter documents;
    private PostgresChunkEmbeddingRepositoryAdapter chunkEmbeddings;

    @BeforeEach
    void setUp() {
        documents = new PostgresDocumentRepositoryAdapter(jdbcTemplate);
        chunkEmbeddings = new PostgresChunkEmbeddingRepositoryAdapter(jdbcTemplate);
    }

    @Test
    void findsDocumentByWorkspaceAndId() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Architecture Notes",
                "URL",
                "https://example.com/architecture",
                "checksum-123",
                "READY",
                Map.of("tag", "architecture"));
        insertDocument(
                UUID.randomUUID(),
                "Other Workspace",
                "TEXT",
                null,
                "checksum-456",
                "READY",
                Map.of());

        Document document = documents.findById(workspaceId, documentId).orElseThrow();

        assertThat(document.id()).isEqualTo(documentId);
        assertThat(document.workspaceId()).isEqualTo(workspaceId);
        assertThat(document.title()).isEqualTo("Architecture Notes");
        assertThat(document.source().type()).isEqualTo(DocumentSourceType.URL);
        assertThat(document.source().uri()).isEqualTo("https://example.com/architecture");
        assertThat(document.checksum()).isEqualTo("checksum-123");
        assertThat(document.status()).isEqualTo(DocumentStatus.READY);
        assertThat(document.metadata()).containsEntry("tag", "architecture");
        assertThat(documents.findById(UUID.randomUUID(), documentId)).isEmpty();
    }

    @Test
    void updatesDocumentStatusWithinWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(workspaceId, "Architecture Notes", "TEXT", null, "checksum-123", "INGESTION_REQUESTED", Map.of());

        documents.markProcessing(workspaceId, documentId);
        documents.markFailed(workspaceId, documentId, "embedding provider unavailable");

        assertThat(readDocumentStatus(workspaceId, documentId)).isEqualTo("FAILED");
        assertThat(readFailureReason(workspaceId, documentId)).isEqualTo("embedding provider unavailable");

        documents.markReady(workspaceId, documentId);

        assertThat(readDocumentStatus(workspaceId, documentId)).isEqualTo("READY");
        assertThat(readFailureReason(workspaceId, documentId)).isNull();
    }

    @Test
    void savesChunkEmbeddingsAndReplacesPreviousDocumentChunks() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(workspaceId, "Architecture Notes", "TEXT", null, "checksum-123", "PROCESSING", Map.of());
        Chunk firstChunk = chunk(workspaceId, documentId, 0, "Ports isolate adapters.", Map.of("section", "architecture"));
        Chunk secondChunk = chunk(workspaceId, documentId, 1, "Messaging handles async work.", Map.of("section", "messaging"));

        chunkEmbeddings.saveAll(
                workspaceId,
                documentId,
                List.of(
                        ChunkEmbedding.of(firstChunk, EmbeddingVector.of(List.of(1.0, 0.0, 0.0), "test-model")),
                        ChunkEmbedding.of(secondChunk, EmbeddingVector.of(List.of(0.0, 1.0, 0.0), "test-model"))));

        assertThat(countRows("chunks")).isEqualTo(2);
        assertThat(countRows("chunk_embeddings")).isEqualTo(2);
        assertThat(readChunkMetadata(firstChunk.id(), "section")).isEqualTo("architecture");

        Chunk replacement = chunk(workspaceId, documentId, 0, "Updated architecture note.", Map.of("section", "updated"));

        chunkEmbeddings.saveAll(
                workspaceId,
                documentId,
                List.of(ChunkEmbedding.of(replacement, EmbeddingVector.of(List.of(0.5, 0.5, 0.0), "test-model-v2"))));

        assertThat(countRows("chunks")).isEqualTo(1);
        assertThat(countRows("chunk_embeddings")).isEqualTo(1);
        assertThat(readChunkContent(replacement.id())).isEqualTo("Updated architecture note.");
        assertThat(readEmbeddingModel(replacement.id())).isEqualTo("test-model-v2");
    }

    @Test
    void rejectsChunkOutsideRepositoryScope() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(workspaceId, "Architecture Notes", "TEXT", null, "checksum-123", "PROCESSING", Map.of());
        Chunk validChunk = chunk(workspaceId, documentId, 0, "Existing scope.", Map.of());
        Chunk wrongWorkspaceChunk = chunk(UUID.randomUUID(), documentId, 0, "Wrong scope.", Map.of());

        chunkEmbeddings.saveAll(
                workspaceId,
                documentId,
                List.of(ChunkEmbedding.of(validChunk, EmbeddingVector.of(List.of(1.0), "test-model"))));

        assertThatThrownBy(() -> chunkEmbeddings.saveAll(
                workspaceId,
                documentId,
                List.of(ChunkEmbedding.of(wrongWorkspaceChunk, EmbeddingVector.of(List.of(1.0), "test-model")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chunk scope must match repository scope");
        assertThat(countRows("chunks")).isEqualTo(1);
        assertThat(readChunkContent(validChunk.id())).isEqualTo("Existing scope.");
    }

    private UUID insertDocument(
            UUID workspaceId,
            String title,
            String sourceType,
            String sourceUri,
            String checksum,
            String status,
            Map<String, String> metadata) {
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO documents (id, workspace_id, title, source_type, source_uri, checksum, status, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """,
                documentId,
                workspaceId,
                title,
                sourceType,
                sourceUri,
                checksum,
                status,
                JsonbMetadata.write(metadata));
        return documentId;
    }

    private static Chunk chunk(UUID workspaceId, UUID documentId, int index, String content, Map<String, String> metadata) {
        return Chunk.create(workspaceId, documentId, index, content, content.split("\\s+").length, metadata);
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    private String readDocumentStatus(UUID workspaceId, UUID documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM documents WHERE workspace_id = ? AND id = ?",
                String.class,
                workspaceId,
                documentId);
    }

    private String readFailureReason(UUID workspaceId, UUID documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT failure_reason FROM documents WHERE workspace_id = ? AND id = ?",
                String.class,
                workspaceId,
                documentId);
    }

    private String readChunkMetadata(UUID chunkId, String key) {
        return jdbcTemplate.queryForObject(
                "SELECT metadata ->> ? FROM chunks WHERE id = ?",
                String.class,
                key,
                chunkId);
    }

    private String readChunkContent(UUID chunkId) {
        return jdbcTemplate.queryForObject(
                "SELECT content FROM chunks WHERE id = ?",
                String.class,
                chunkId);
    }

    private String readEmbeddingModel(UUID chunkId) {
        return jdbcTemplate.queryForObject(
                "SELECT model FROM chunk_embeddings WHERE chunk_id = ?",
                String.class,
                chunkId);
    }
}
