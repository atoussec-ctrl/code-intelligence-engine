package com.rag.rag.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.rag.adapter.out.messaging.RabbitDocumentProcessingPublisher;
import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import com.rag.rag.application.usecase.ProcessDocumentResult;
import com.rag.rag.application.usecase.ProcessDocumentUseCase;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
	"spring.flyway.enabled=true",
	"spring.rabbitmq.listener.simple.auto-startup=true",
	"spring.rabbitmq.listener.simple.retry.initial-interval=10ms",
	"spring.rabbitmq.listener.simple.retry.multiplier=1",
	"spring.rabbitmq.listener.simple.retry.max-interval=10ms",
	"rag.document-processing.dispatcher.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class DocumentProcessingLifecycleIntegrationTest {

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
		DockerImageName.parse("pgvector/pgvector:pg16"));

	@Container
	private static final RabbitMQContainer RABBITMQ =
		new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

	@Autowired
	private DocumentProcessingRequestPort processingRequests;

	@Autowired
	private RabbitDocumentProcessingPublisher publisher;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private ProcessDocumentUseCase processDocument;

	@DynamicPropertySource
	static void infrastructureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
	}

	@BeforeEach
	void resetInfrastructure() {
		rabbitAdmin.purgeQueue(RabbitDocumentProcessingConfig.PROCESSING_QUEUE, false);
		rabbitAdmin.purgeQueue(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE, false);
		jdbcTemplate.update(
			"TRUNCATE TABLE document_processing_requests, chunk_embeddings, chunks, documents CASCADE");
	}

	@Test
	void processesDuplicateDeliveriesOnlyOnce() {
		var command = createProcessingRequest("Idempotent content.");
		var requestId = requestId(command);
		when(processDocument.execute(command))
			.thenReturn(new ProcessDocumentResult(command.documentId(), 1));

		publisher.publish(requestId);
		publisher.publish(requestId);

		await().atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> assertThat(status(requestId)).isEqualTo(DocumentProcessingStatus.COMPLETED));
		verify(processDocument, after(Duration.ofSeconds(1).toMillis()).times(1)).execute(command);
		assertThat(processingAttempts(requestId)).isEqualTo(1);
	}

	@Test
	void persistsFailureAndDeadLettersMessageAfterRetriesAreExhausted() {
		var command = createProcessingRequest("Content that fails.");
		var requestId = requestId(command);
		doThrow(new IllegalStateException("embedding provider unavailable"))
			.when(processDocument).execute(command);

		publisher.publish(requestId);

		await().atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> assertThat(status(requestId)).isEqualTo(DocumentProcessingStatus.FAILED));
		verify(processDocument, after(Duration.ofSeconds(1).toMillis()).times(3)).execute(command);
		assertThat(processingAttempts(requestId)).isEqualTo(3);
		assertThat(lastError(requestId)).isEqualTo("embedding provider unavailable");
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
			rabbitTemplate.receiveAndConvert(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE))
			.isEqualTo(new DocumentProcessingMessage(requestId)));
	}

	private ProcessDocumentCommand createProcessingRequest(String content) {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		jdbcTemplate.update(
			"""
			INSERT INTO documents (
				id, workspace_id, title, source_type, checksum, status, metadata)
			VALUES (?, ?, 'Lifecycle integration', 'TEXT', 'integration-checksum',
				'INGESTION_REQUESTED', '{}'::jsonb)
			""",
			documentId,
			workspaceId);
		return new ProcessDocumentCommand(workspaceId, documentId, content, 256);
	}

	private UUID requestId(ProcessDocumentCommand command) {
		return processingRequests.create(command);
	}

	private DocumentProcessingStatus status(UUID requestId) {
		return DocumentProcessingStatus.valueOf(jdbcTemplate.queryForObject(
			"SELECT status FROM document_processing_requests WHERE id = ?",
			String.class,
			requestId));
	}

	private int processingAttempts(UUID requestId) {
		return jdbcTemplate.queryForObject(
			"SELECT processing_attempts FROM document_processing_requests WHERE id = ?",
			Integer.class,
			requestId);
	}

	private String lastError(UUID requestId) {
		return jdbcTemplate.queryForObject(
			"SELECT last_error FROM document_processing_requests WHERE id = ?",
			String.class,
			requestId);
	}

}
