package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rag.rag.application.port.out.DocumentProcessingRetryOutcome;
import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import com.rag.rag.domain.document.Chunk;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresPersistenceAdaptersIntegrationTest extends PgVectorIntegrationTestSupport {

    private PostgresDocumentRepositoryAdapter documents;
    private PostgresChunkEmbeddingRepositoryAdapter chunkEmbeddings;
    private PostgresDocumentProcessingRequestAdapter processingRequests;

    @BeforeEach
    void setUp() {
        documents = new PostgresDocumentRepositoryAdapter(jdbcTemplate);
        chunkEmbeddings = new PostgresChunkEmbeddingRepositoryAdapter(jdbcTemplate);
        processingRequests = new PostgresDocumentProcessingRequestAdapter(jdbcTemplate);
    }

    @Test
    void savesDocumentAndFindsItByWorkspaceAndId() {
        UUID workspaceId = UUID.randomUUID();
        Document document = Document.create(
                workspaceId,
                "Architecture Notes",
                DocumentSource.text(),
                "checksum-123",
                Map.of("tag", "architecture"));

        Document saved = documents.save(document);
        Document found = documents.findById(workspaceId, saved.id()).orElseThrow();

        assertThat(saved).isSameAs(document);
        assertThat(found.id()).isEqualTo(document.id());
        assertThat(found.workspaceId()).isEqualTo(workspaceId);
        assertThat(found.title()).isEqualTo("Architecture Notes");
        assertThat(found.source().type()).isEqualTo(DocumentSourceType.TEXT);
        assertThat(found.source().uri()).isNull();
        assertThat(found.checksum()).isEqualTo("checksum-123");
        assertThat(found.status()).isEqualTo(DocumentStatus.INGESTION_REQUESTED);
        assertThat(found.metadata()).containsEntry("tag", "architecture");
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

    @Test
    void persistsClaimsPublishesAndCompletesDocumentProcessingRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Architecture Notes",
                "TEXT",
                null,
                "checksum-123",
                "INGESTION_REQUESTED",
                Map.of());
        ProcessDocumentCommand command = new ProcessDocumentCommand(
                workspaceId,
                documentId,
                "Durable processing content.",
                256);

        UUID requestId = processingRequests.create(command);

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PENDING");
        assertThat(processingRequests.findById(workspaceId, documentId, requestId))
                .get()
                .extracting(state -> state.status())
                .isEqualTo(DocumentProcessingStatus.PENDING);
        assertThat(processingRequests.findById(UUID.randomUUID(), documentId, requestId)).isEmpty();

        List<ClaimedDocumentProcessingRequest> claimed =
                processingRequests.claimPending(10, Duration.ofSeconds(30));

        assertThat(claimed).containsExactly(new ClaimedDocumentProcessingRequest(requestId, 1));
        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("DISPATCHING");
        assertThat(processingRequests.claimPending(10, Duration.ofSeconds(30))).isEmpty();

        processingRequests.markPublished(requestId);

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PUBLISHED");
        assertThat(readTimestamp("published_at", requestId)).isNotNull();

        var processingClaim = processingRequests.claimForProcessing(requestId, Duration.ofMinutes(30));

        assertThat(processingClaim).get().matches(claim -> claim.acquired());
        assertThat(processingClaim.orElseThrow().command()).isEqualTo(command);
        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PROCESSING");
        assertThat(processingRequests.claimForProcessing(requestId, Duration.ofMinutes(30)))
                .get()
                .matches(claim -> !claim.acquired())
                .extracting(claim -> claim.status())
                .isEqualTo(DocumentProcessingStatus.PROCESSING);

        processingRequests.markCompleted(requestId);

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("COMPLETED");
        assertThat(readTimestamp("completed_at", requestId)).isNotNull();
        assertThat(readProcessingRequestContent(requestId)).isNull();
        assertThat(processingRequests.claimForProcessing(requestId, Duration.ofMinutes(30)))
                .get()
                .matches(claim -> !claim.acquired())
                .extracting(claim -> claim.status())
                .isEqualTo(DocumentProcessingStatus.COMPLETED);
    }

    @Test
    void reschedulesPublicationFailureAndReclaimsExpiredLease() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Architecture Notes",
                "TEXT",
                null,
                "checksum-123",
                "INGESTION_REQUESTED",
                Map.of());
        UUID requestId = processingRequests.create(new ProcessDocumentCommand(
                workspaceId,
                documentId,
                "Retry processing content.",
                256));
        processingRequests.claimPending(1, Duration.ofSeconds(30));

        processingRequests.reschedule(requestId, "connection refused", Duration.ofMinutes(1));

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PENDING");
        assertThat(readProcessingRequestError(requestId)).isEqualTo("connection refused");
        assertThat(processingRequests.claimPending(1, Duration.ofSeconds(30))).isEmpty();

        jdbcTemplate.update(
                "UPDATE document_processing_requests SET available_at = now() - interval '1 second' WHERE id = ?",
                requestId);
        assertThat(processingRequests.claimPending(1, Duration.ofMillis(1)))
                .containsExactly(new ClaimedDocumentProcessingRequest(requestId, 2));

        jdbcTemplate.update(
                "UPDATE document_processing_requests SET lease_until = now() - interval '1 second' WHERE id = ?",
                requestId);
        assertThat(processingRequests.claimPending(1, Duration.ofSeconds(30)))
                .containsExactly(new ClaimedDocumentProcessingRequest(requestId, 3));
    }

    @Test
    void releasesFailedProcessingAndReclaimsExpiredProcessingLease() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Architecture Notes",
                "TEXT",
                null,
                "checksum-123",
                "INGESTION_REQUESTED",
                Map.of());
        UUID requestId = processingRequests.create(new ProcessDocumentCommand(
                workspaceId,
                documentId,
                "Retry processing content.",
                256));

        assertThat(processingRequests.claimForProcessing(requestId, Duration.ofMinutes(30)))
                .get()
                .matches(claim -> claim.acquired());
        processingRequests.releaseForRetry(requestId, "embedding provider unavailable");

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PUBLISHED");
        assertThat(readProcessingRequestError(requestId)).isEqualTo("embedding provider unavailable");
        assertThat(processingRequests.claimForProcessing(requestId, Duration.ofMillis(1)))
                .get()
                .matches(claim -> claim.acquired());

        jdbcTemplate.update(
                "UPDATE document_processing_requests SET lease_until = now() - interval '1 second' WHERE id = ?",
                requestId);

        assertThat(processingRequests.claimPending(1, Duration.ofSeconds(30)))
                .containsExactly(new ClaimedDocumentProcessingRequest(requestId, 1));

        processingRequests.markPublished(requestId);
        processingRequests.markFailed(requestId, "retry attempts exhausted");

        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("FAILED");
        assertThat(readProcessingRequestError(requestId)).isEqualTo("retry attempts exhausted");
        assertThat(readTimestamp("failed_at", requestId)).isNotNull();
        assertThat(readProcessingRequestContent(requestId)).isEqualTo("Retry processing content.");
        assertThat(processingRequests.claimForProcessing(requestId, Duration.ofMinutes(30)))
                .get()
                .extracting(claim -> claim.status())
                .isEqualTo(DocumentProcessingStatus.FAILED);

        assertThat(processingRequests.retryFailed(UUID.randomUUID(), documentId, requestId))
                .isEqualTo(DocumentProcessingRetryOutcome.NOT_FOUND);
        assertThat(processingRequests.retryFailed(workspaceId, documentId, requestId))
                .isEqualTo(DocumentProcessingRetryOutcome.RETRIED);
        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PENDING");
        assertThat(readProcessingRequestError(requestId)).isNull();
        assertThat(readTimestamp("failed_at", requestId)).isNull();
        assertThat(processingRequests.retryFailed(workspaceId, documentId, requestId))
                .isEqualTo(DocumentProcessingRetryOutcome.NOT_FAILED);
    }

    @Test
    void grantsOnlyOneProcessingClaimAcrossConcurrentConsumers() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Concurrent Architecture Notes",
                "TEXT",
                null,
                "checksum-concurrent",
                "INGESTION_REQUESTED",
                Map.of());
        UUID requestId = processingRequests.create(new ProcessDocumentCommand(
                workspaceId,
                documentId,
                "Concurrent processing content.",
                256));
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return new PostgresDocumentProcessingRequestAdapter(jdbcTemplate)
                        .claimForProcessing(requestId, Duration.ofMinutes(30))
                        .orElseThrow();
            });
            var second = executor.submit(() -> {
                start.await();
                return new PostgresDocumentProcessingRequestAdapter(jdbcTemplate)
                        .claimForProcessing(requestId, Duration.ofMinutes(30))
                        .orElseThrow();
            });

            start.countDown();
            var claims = List.of(first.get(), second.get());

            assertThat(claims).filteredOn(claim -> claim.acquired()).hasSize(1);
            assertThat(claims).filteredOn(claim -> !claim.acquired()).hasSize(1);
            assertThat(claims).allMatch(claim -> claim.status() == DocumentProcessingStatus.PROCESSING);

            processingRequests.markFailed(requestId, "stale delivery failure");

            assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PROCESSING");
        }
    }

    @Test
    void retriesFailedRequestOnlyOnceAcrossConcurrentCallers() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = insertDocument(
                workspaceId,
                "Concurrent Retry Notes",
                "TEXT",
                null,
                "checksum-concurrent-retry",
                "INGESTION_REQUESTED",
                Map.of());
        UUID requestId = processingRequests.create(new ProcessDocumentCommand(
                workspaceId,
                documentId,
                "Concurrent retry content.",
                256));
        processingRequests.markFailed(requestId, "provider unavailable");
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return new PostgresDocumentProcessingRequestAdapter(jdbcTemplate)
                        .retryFailed(workspaceId, documentId, requestId);
            });
            var second = executor.submit(() -> {
                start.await();
                return new PostgresDocumentProcessingRequestAdapter(jdbcTemplate)
                        .retryFailed(workspaceId, documentId, requestId);
            });

            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(
                    DocumentProcessingRetryOutcome.RETRIED,
                    DocumentProcessingRetryOutcome.NOT_FAILED);
        }
        assertThat(readProcessingRequestStatus(requestId)).isEqualTo("PENDING");
        assertThat(readProcessingRequestContent(requestId)).isEqualTo("Concurrent retry content.");
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

    private String readProcessingRequestStatus(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM document_processing_requests WHERE id = ?",
                String.class,
                requestId);
    }

    private String readProcessingRequestError(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM document_processing_requests WHERE id = ?",
                String.class,
                requestId);
    }

    private String readProcessingRequestContent(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT content FROM document_processing_requests WHERE id = ?",
                String.class,
                requestId);
    }

    private java.time.OffsetDateTime readTimestamp(String column, UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM document_processing_requests WHERE id = ?",
                java.time.OffsetDateTime.class,
                requestId);
    }
}
