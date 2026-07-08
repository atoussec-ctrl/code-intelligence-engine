package com.rag.rag.adapter.out.persistence;

import com.rag.rag.application.port.out.VectorSearchPort;
import com.rag.rag.application.rag.RetrievedContext;
import com.rag.rag.application.rag.VectorSearchQuery;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PgVectorSearchAdapter implements VectorSearchPort {

    private static final String SEARCH_SQL = """
            SELECT
                c.workspace_id,
                c.id AS chunk_id,
                c.document_id,
                d.title AS source_title,
                c.content,
                1 - (ce.embedding <=> ?::vector) AS score
            FROM chunk_embeddings ce
            JOIN chunks c ON c.id = ce.chunk_id
            JOIN documents d ON d.id = c.document_id AND d.workspace_id = c.workspace_id
            WHERE c.workspace_id = ?
            ORDER BY ce.embedding <=> ?::vector
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PgVectorSearchAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
    }

    @Override
    public List<RetrievedContext> search(VectorSearchQuery query) {
        Objects.requireNonNull(query, "query is required");
        String vectorLiteral = toPgVectorLiteral(query.embedding());
        return jdbcTemplate.query(
                SEARCH_SQL,
                this::mapRow,
                vectorLiteral,
                query.workspaceId(),
                vectorLiteral,
                query.topK());
    }

    private RetrievedContext mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RetrievedContext(
                resultSet.getObject("workspace_id", java.util.UUID.class),
                resultSet.getObject("chunk_id", java.util.UUID.class),
                resultSet.getObject("document_id", java.util.UUID.class),
                resultSet.getString("source_title"),
                resultSet.getString("content"),
                resultSet.getDouble("score"));
    }

    private static String toPgVectorLiteral(EmbeddingVector embedding) {
        return embedding.values().stream()
                .map(PgVectorSearchAdapter::formatVectorValue)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String formatVectorValue(Double value) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException("embedding values must be finite");
        }
        return value.toString();
    }
}
