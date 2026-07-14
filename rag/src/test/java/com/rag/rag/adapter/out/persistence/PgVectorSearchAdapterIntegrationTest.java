package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.rag.rag.application.rag.RetrievedContext;
import com.rag.rag.application.rag.VectorSearchQuery;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class PgVectorSearchAdapterIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(PGVECTOR_IMAGE);

    private JdbcTemplate jdbcTemplate;
    private PgVectorSearchAdapter adapter;

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("TRUNCATE TABLE chunk_embeddings, chunks, documents CASCADE");
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

    private static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
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
