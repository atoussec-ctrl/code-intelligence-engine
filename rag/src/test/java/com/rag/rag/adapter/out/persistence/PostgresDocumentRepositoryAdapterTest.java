package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PostgresDocumentRepositoryAdapterTest {

    @Test
    void rejectsNullDocumentWhenSaving() {
        PostgresDocumentRepositoryAdapter adapter = new PostgresDocumentRepositoryAdapter(
                new RecordingJdbcTemplate(null));

        assertThatThrownBy(() -> adapter.save(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("document is required");
    }

    @Test
    void savesDocumentWithPreparedSql() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(null);
        PostgresDocumentRepositoryAdapter adapter = new PostgresDocumentRepositoryAdapter(jdbcTemplate);
        UUID workspaceId = UUID.randomUUID();
        Document document = Document.create(
                workspaceId,
                "Architecture Notes",
                DocumentSource.url("https://example.com/architecture"),
                "checksum-123",
                java.util.Map.of("tag", "architecture"));

        Document saved = adapter.save(document);

        assertThat(saved).isSameAs(document);
        assertThat(jdbcTemplate.updates()).hasSize(1);
        RecordedUpdate insert = jdbcTemplate.updates().getFirst();
        assertThat(insert.sql()).contains("INSERT INTO documents").contains("?::jsonb");
        assertThat(insert.args()[0]).isEqualTo(document.id());
        assertThat(insert.args()[1]).isEqualTo(workspaceId);
        assertThat(insert.args()[2]).isEqualTo("Architecture Notes");
        assertThat(insert.args()[3]).isEqualTo("URL");
        assertThat(insert.args()[4]).isEqualTo("https://example.com/architecture");
        assertThat(insert.args()[5]).isEqualTo("checksum-123");
        assertThat(insert.args()[6]).isEqualTo("INGESTION_REQUESTED");
        assertThat(JsonbMetadata.read((String) insert.args()[7]))
                .containsEntry("tag", "architecture");
    }

    @Test
    void findsDocumentByIdWithMappedJsonMetadata() throws SQLException {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(documentId);
        when(resultSet.getObject("workspace_id", UUID.class)).thenReturn(workspaceId);
        when(resultSet.getString("title")).thenReturn("Architecture Notes");
        when(resultSet.getString("source_type")).thenReturn("URL");
        when(resultSet.getString("source_uri")).thenReturn("https://example.com/architecture");
        when(resultSet.getString("checksum")).thenReturn("checksum-123");
        when(resultSet.getString("status")).thenReturn("READY");
        when(resultSet.getString("metadata")).thenReturn("{\"tag\":\"architecture\"}");
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(resultSet);
        PostgresDocumentRepositoryAdapter adapter = new PostgresDocumentRepositoryAdapter(jdbcTemplate);

        var document = adapter.findById(workspaceId, documentId).orElseThrow();

        assertThat(document.id()).isEqualTo(documentId);
        assertThat(document.workspaceId()).isEqualTo(workspaceId);
        assertThat(document.title()).isEqualTo("Architecture Notes");
        assertThat(document.source().type()).isEqualTo(DocumentSourceType.URL);
        assertThat(document.source().uri()).isEqualTo("https://example.com/architecture");
        assertThat(document.checksum()).isEqualTo("checksum-123");
        assertThat(document.status()).isEqualTo(DocumentStatus.READY);
        assertThat(document.metadata()).containsEntry("tag", "architecture");
        assertThat(jdbcTemplate.querySql()).contains("FROM documents").contains("WHERE workspace_id = ? AND id = ?");
        assertThat(jdbcTemplate.queryArgs()).containsExactly(workspaceId, documentId);
    }

    @Test
    void returnsEmptyWhenDocumentIsNotFound() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(null);
        PostgresDocumentRepositoryAdapter adapter = new PostgresDocumentRepositoryAdapter(jdbcTemplate);

        assertThat(adapter.findById(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatesDocumentStatusTransitions() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(null);
        PostgresDocumentRepositoryAdapter adapter = new PostgresDocumentRepositoryAdapter(jdbcTemplate);
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        adapter.markProcessing(workspaceId, documentId);
        adapter.markFailed(workspaceId, documentId, "embedding provider unavailable");
        adapter.markReady(workspaceId, documentId);

        assertThat(jdbcTemplate.updates()).hasSize(3);
        assertThat(jdbcTemplate.updates().get(0).args())
                .containsExactly("PROCESSING", null, workspaceId, documentId);
        assertThat(jdbcTemplate.updates().get(1).args())
                .containsExactly("FAILED", "embedding provider unavailable", workspaceId, documentId);
        assertThat(jdbcTemplate.updates().get(2).args())
                .containsExactly("READY", null, workspaceId, documentId);
        assertThat(jdbcTemplate.updates().get(0).sql())
                .contains("UPDATE documents")
                .contains("updated_at = now()");
    }

    @Test
    void requiresJdbcTemplate() {
        assertThatThrownBy(() -> new PostgresDocumentRepositoryAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate is required");
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final ResultSet resultSet;
        private final List<RecordedUpdate> updates = new ArrayList<>();
        private String querySql;
        private Object[] queryArgs;

        private RecordingJdbcTemplate(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.querySql = sql;
            this.queryArgs = args.clone();
            if (resultSet == null) {
                return List.of();
            }
            try {
                return List.of(rowMapper.mapRow(resultSet, 0));
            } catch (SQLException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public int update(String sql, Object... args) {
            updates.add(new RecordedUpdate(sql, args.clone()));
            return 1;
        }

        private String querySql() {
            return querySql;
        }

        private Object[] queryArgs() {
            return queryArgs.clone();
        }

        private List<RecordedUpdate> updates() {
            return List.copyOf(updates);
        }
    }

    private record RecordedUpdate(String sql, Object[] args) {
    }
}
