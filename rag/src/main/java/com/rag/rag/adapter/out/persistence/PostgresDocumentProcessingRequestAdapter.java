package com.rag.rag.adapter.out.persistence;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDocumentProcessingRequestAdapter implements DocumentProcessingRequestPort {

	private static final int MAX_ERROR_LENGTH = 2000;

	private final JdbcTemplate jdbcTemplate;

	public PostgresDocumentProcessingRequestAdapter(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
	}

	@Override
	public UUID create(ProcessDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		var requestId = UUID.randomUUID();
		jdbcTemplate.update(
			"""
			INSERT INTO document_processing_requests (
				id, workspace_id, document_id, content, max_tokens, status)
			VALUES (?, ?, ?, ?, ?, 'PENDING')
			""",
			requestId,
			command.workspaceId(),
			command.documentId(),
			command.content(),
			command.maxTokens());
		return requestId;
	}

	@Override
	public Optional<ProcessDocumentCommand> findCommandById(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		return jdbcTemplate.query(
			"""
			SELECT workspace_id, document_id, content, max_tokens
			FROM document_processing_requests
			WHERE id = ?
			""",
			(resultSet, rowNumber) -> new ProcessDocumentCommand(
				resultSet.getObject("workspace_id", UUID.class),
				resultSet.getObject("document_id", UUID.class),
				resultSet.getString("content"),
				resultSet.getInt("max_tokens")),
			requestId).stream().findFirst();
	}

	@Override
	public void markCompleted(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'COMPLETED', completed_at = now(), lease_until = NULL, updated_at = now()
			WHERE id = ?
			""",
			requestId);
	}

	public List<ClaimedDocumentProcessingRequest> claimPending(int batchSize, Duration leaseDuration) {
		if (batchSize <= 0) {
			throw new IllegalArgumentException("batch size must be positive");
		}
		requirePositive(leaseDuration, "lease duration must be positive");
		return jdbcTemplate.query(
			"""
			WITH claimable AS (
				SELECT id
				FROM document_processing_requests
				WHERE (status = 'PENDING' AND available_at <= now())
					OR (status = 'DISPATCHING' AND lease_until <= now())
				ORDER BY created_at, id
				FOR UPDATE SKIP LOCKED
				LIMIT ?
			)
			UPDATE document_processing_requests AS request
			SET status = 'DISPATCHING',
				attempts = request.attempts + 1,
				lease_until = now() + (CAST(? AS bigint) * interval '1 millisecond'),
				updated_at = now()
			FROM claimable
			WHERE request.id = claimable.id
			RETURNING request.id, request.attempts
			""",
			(resultSet, rowNumber) -> new ClaimedDocumentProcessingRequest(
				resultSet.getObject("id", UUID.class),
				resultSet.getInt("attempts")),
			batchSize,
			leaseDuration.toMillis());
	}

	public void markPublished(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'PUBLISHED', published_at = now(), lease_until = NULL,
				last_error = NULL, updated_at = now()
			WHERE id = ? AND status = 'DISPATCHING'
			""",
			requestId);
	}

	public void reschedule(UUID requestId, String error, Duration delay) {
		Objects.requireNonNull(requestId, "request id is required");
		requirePositive(delay, "retry delay must be positive");
		var safeError = error == null || error.isBlank() ? "Unknown publication failure" : error;
		if (safeError.length() > MAX_ERROR_LENGTH) {
			safeError = safeError.substring(0, MAX_ERROR_LENGTH);
		}
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'PENDING',
				available_at = now() + (CAST(? AS bigint) * interval '1 millisecond'),
				lease_until = NULL, last_error = ?, updated_at = now()
			WHERE id = ? AND status = 'DISPATCHING'
			""",
			delay.toMillis(),
			safeError,
			requestId);
	}

	private static void requirePositive(Duration duration, String message) {
		Objects.requireNonNull(duration, message);
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(message);
		}
	}

}
