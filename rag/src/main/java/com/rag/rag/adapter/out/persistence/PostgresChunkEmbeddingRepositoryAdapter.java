package com.rag.rag.adapter.out.persistence;

import com.rag.rag.application.port.out.ChunkEmbeddingRepositoryPort;
import com.rag.rag.domain.document.Chunk;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresChunkEmbeddingRepositoryAdapter implements ChunkEmbeddingRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public PostgresChunkEmbeddingRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
    }

    @Override
    @Transactional
    public void saveAll(UUID workspaceId, UUID documentId, List<ChunkEmbedding> chunks) {
        Objects.requireNonNull(workspaceId, "workspaceId is required");
        Objects.requireNonNull(documentId, "documentId is required");
        List<ChunkEmbedding> safeChunks = List.copyOf(Objects.requireNonNull(chunks, "chunks are required"));

        safeChunks.forEach(chunkEmbedding -> ensureDocumentScope(workspaceId, documentId, chunkEmbedding.chunk()));

        jdbcTemplate.update(
                "DELETE FROM chunks WHERE workspace_id = ? AND document_id = ?",
                workspaceId,
                documentId);

        for (ChunkEmbedding chunkEmbedding : safeChunks) {
            insertChunk(chunkEmbedding.chunk());
            insertEmbedding(chunkEmbedding);
        }
    }

    private void insertChunk(Chunk chunk) {
        jdbcTemplate.update(
                """
                INSERT INTO chunks (id, workspace_id, document_id, chunk_index, content, token_count, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                """,
                chunk.id(),
                chunk.workspaceId(),
                chunk.documentId(),
                chunk.chunkIndex(),
                chunk.content(),
                chunk.tokenCount(),
                JsonbMetadata.write(chunk.metadata()));
    }

    private void insertEmbedding(ChunkEmbedding chunkEmbedding) {
        Chunk chunk = chunkEmbedding.chunk();
        jdbcTemplate.update(
                """
                INSERT INTO chunk_embeddings (chunk_id, workspace_id, document_id, model, embedding)
                VALUES (?, ?, ?, ?, ?::vector)
                """,
                chunk.id(),
                chunk.workspaceId(),
                chunk.documentId(),
                chunkEmbedding.embedding().model(),
                PgVectorLiteral.from(chunkEmbedding.embedding()));
    }

    private static void ensureDocumentScope(UUID workspaceId, UUID documentId, Chunk chunk) {
        if (!workspaceId.equals(chunk.workspaceId()) || !documentId.equals(chunk.documentId())) {
            throw new IllegalArgumentException("chunk scope must match repository scope");
        }
    }
}
