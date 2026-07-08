package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rag.rag.application.rag.RetrievedContext;
import com.rag.rag.application.rag.VectorSearchQuery;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PgVectorSearchAdapterTest {

    @Test
    void searchesSimilarChunksWithPreparedPgvectorQuery() {
        UUID workspaceId = UUID.randomUUID();
        RetrievedContext context = context(workspaceId);
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(List.of(context));
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(jdbcTemplate);

        List<RetrievedContext> results = adapter.search(new VectorSearchQuery(
                workspaceId,
                EmbeddingVector.of(List.of(0.1, -2.0, 3.5), "test-model"),
                3));

        assertThat(results).containsExactly(context);
        assertThat(jdbcTemplate.sql())
                .contains("WHERE c.workspace_id = ?")
                .contains("ORDER BY ce.embedding <=> ?::vector")
                .doesNotContain(workspaceId.toString())
                .doesNotContain("[0.1,-2.0,3.5]");
        assertThat(jdbcTemplate.args()).containsExactly("[0.1,-2.0,3.5]", workspaceId, "[0.1,-2.0,3.5]", 3);
    }

    @Test
    void mapsRowsToRetrievedContext() throws SQLException {
        UUID workspaceId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(List.of());
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(jdbcTemplate);
        adapter.search(new VectorSearchQuery(
                workspaceId,
                EmbeddingVector.of(List.of(0.1, 0.2), "test-model"),
                1));

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("workspace_id", UUID.class)).thenReturn(workspaceId);
        when(resultSet.getObject("chunk_id", UUID.class)).thenReturn(chunkId);
        when(resultSet.getObject("document_id", UUID.class)).thenReturn(documentId);
        when(resultSet.getString("source_title")).thenReturn("Architecture Notes");
        when(resultSet.getString("content")).thenReturn("Ports isolate infrastructure.");
        when(resultSet.getDouble("score")).thenReturn(0.87);

        RetrievedContext mapped = jdbcTemplate.rowMapper().mapRow(resultSet, 0);

        assertThat(mapped).isEqualTo(new RetrievedContext(
                workspaceId,
                chunkId,
                documentId,
                "Architecture Notes",
                "Ports isolate infrastructure.",
                0.87));
    }

    @Test
    void requiresJdbcTemplate() {
        assertThatThrownBy(() -> new PgVectorSearchAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate is required");
    }

    private static RetrievedContext context(UUID workspaceId) {
        return new RetrievedContext(
                workspaceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Architecture Notes",
                "Ports isolate infrastructure.",
                0.91);
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<RetrievedContext> results;
        private String sql;
        private Object[] args;
        private RowMapper<RetrievedContext> rowMapper;

        private RecordingJdbcTemplate(List<RetrievedContext> results) {
            this.results = List.copyOf(results);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            this.args = args.clone();
            this.rowMapper = (RowMapper<RetrievedContext>) rowMapper;
            return (List<T>) results;
        }

        private String sql() {
            return sql;
        }

        private Object[] args() {
            return args.clone();
        }

        private RowMapper<RetrievedContext> rowMapper() {
            return rowMapper;
        }
    }
}
