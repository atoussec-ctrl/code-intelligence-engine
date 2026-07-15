package com.rag.rag.adapter.out.persistence;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDocumentRepositoryAdapter implements DocumentRepositoryPort {

    private static final String FIND_BY_ID_SQL = """
            SELECT id, workspace_id, title, source_type, source_uri, checksum, status, metadata::text
            FROM documents
            WHERE workspace_id = ? AND id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresDocumentRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
    }

    @Override
    public Document save(Document document) {
        Objects.requireNonNull(document, "document is required");
        jdbcTemplate.update(
                """
                INSERT INTO documents (id, workspace_id, title, source_type, source_uri, checksum, status, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """,
                document.id(),
                document.workspaceId(),
                document.title(),
                document.source().type().name(),
                document.source().uri(),
                document.checksum(),
                document.status().name(),
                JsonbMetadata.write(document.metadata()));
        return document;
    }

    @Override
    public Optional<Document> findById(UUID workspaceId, UUID documentId) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, this::mapDocument, workspaceId, documentId)
                .stream()
                .findFirst();
    }

    @Override
    public void markProcessing(UUID workspaceId, UUID documentId) {
        updateStatus(workspaceId, documentId, DocumentStatus.PROCESSING, null);
    }

    @Override
    public void markReady(UUID workspaceId, UUID documentId) {
        updateStatus(workspaceId, documentId, DocumentStatus.READY, null);
    }

    @Override
    public void markFailed(UUID workspaceId, UUID documentId, String reason) {
        updateStatus(workspaceId, documentId, DocumentStatus.FAILED, reason);
    }

    private void updateStatus(UUID workspaceId, UUID documentId, DocumentStatus status, String failureReason) {
        jdbcTemplate.update(
                """
                UPDATE documents
                SET status = ?, failure_reason = ?, updated_at = now()
                WHERE workspace_id = ? AND id = ?
                """,
                status.name(),
                failureReason,
                workspaceId,
                documentId);
    }

    private Document mapDocument(ResultSet resultSet, @SuppressWarnings("unused") int rowNumber) throws SQLException {
        return Document.restore(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("workspace_id", UUID.class),
                resultSet.getString("title"),
                new DocumentSource(
                        DocumentSourceType.valueOf(resultSet.getString("source_type")),
                        resultSet.getString("source_uri")),
                resultSet.getString("checksum"),
                DocumentStatus.valueOf(resultSet.getString("status")),
                JsonbMetadata.read(resultSet.getString("metadata")));
    }
}
