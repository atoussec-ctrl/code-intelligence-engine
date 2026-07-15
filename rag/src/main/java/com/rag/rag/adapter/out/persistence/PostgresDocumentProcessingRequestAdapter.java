package com.rag.rag.adapter.out.persistence;

import com.rag.rag.application.port.out.DocumentProcessingClaim;
import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentProcessingRequestState;
import com.rag.rag.application.port.out.DocumentProcessingRetryOutcome;
import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.time.Duration;
import java.time.Instant;
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
	public Optional<DocumentProcessingClaim> claimForProcessing(
		UUID requestId,
		Duration leaseDuration) {
		Objects.requireNonNull(requestId, "request id is required");
		requirePositive(leaseDuration, "processing lease duration must be positive");
		var acquired = jdbcTemplate.query(
			"""
			UPDATE document_processing_requests
			SET status = 'PROCESSING',
				processing_attempts = processing_attempts + 1,
				lease_until = now() + (CAST(? AS bigint) * interval '1 millisecond'),
				updated_at = now()
			WHERE id = ? AND status IN ('PENDING', 'DISPATCHING', 'PUBLISHED')
			RETURNING workspace_id, document_id, content, max_tokens
			""",
			(resultSet, rowNumber) -> DocumentProcessingClaim.acquired(toCommand(resultSet)),
			leaseDuration.toMillis(),
			requestId).stream().findFirst();
		if (acquired.isPresent()) {
			return acquired;
		}
		return jdbcTemplate.query(
			"SELECT status FROM document_processing_requests WHERE id = ?",
			(resultSet, rowNumber) -> DocumentProcessingClaim.notAcquired(
				DocumentProcessingStatus.valueOf(resultSet.getString("status"))),
			requestId).stream().findFirst();
	}

	@Override
	public Optional<DocumentProcessingRequestState> findById(
		UUID workspaceId,
		UUID documentId,
		UUID requestId) {
		Objects.requireNonNull(workspaceId, "workspace id is required");
		Objects.requireNonNull(documentId, "document id is required");
		Objects.requireNonNull(requestId, "request id is required");
		return jdbcTemplate.query(
			"""
			SELECT id, workspace_id, document_id, status, dispatch_attempts,
				processing_attempts, last_error, created_at, updated_at,
				published_at, completed_at, failed_at
			FROM document_processing_requests
			WHERE id = ? AND workspace_id = ? AND document_id = ?
			""",
			(resultSet, rowNumber) -> new DocumentProcessingRequestState(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("workspace_id", UUID.class),
				resultSet.getObject("document_id", UUID.class),
				DocumentProcessingStatus.valueOf(resultSet.getString("status")),
				resultSet.getInt("dispatch_attempts"),
				resultSet.getInt("processing_attempts"),
				resultSet.getString("last_error"),
				toInstant(resultSet, "created_at"),
				toInstant(resultSet, "updated_at"),
				toNullableInstant(resultSet, "published_at"),
				toNullableInstant(resultSet, "completed_at"),
				toNullableInstant(resultSet, "failed_at")),
			requestId,
			workspaceId,
			documentId).stream().findFirst();
	}

	@Override
	public DocumentProcessingRetryOutcome retryFailed(
		UUID workspaceId,
		UUID documentId,
		UUID requestId) {
		Objects.requireNonNull(workspaceId, "workspace id is required");
		Objects.requireNonNull(documentId, "document id is required");
		Objects.requireNonNull(requestId, "request id is required");
		return jdbcTemplate.queryForObject(
			"""
			WITH retried AS (
				UPDATE document_processing_requests
				SET status = 'PENDING', available_at = now(), lease_until = NULL,
					last_error = NULL, published_at = NULL, completed_at = NULL,
					failed_at = NULL, updated_at = now()
				WHERE id = ? AND workspace_id = ? AND document_id = ? AND status = 'FAILED'
				RETURNING id
			)
			SELECT CASE
				WHEN EXISTS (SELECT 1 FROM retried) THEN 'RETRIED'
				WHEN EXISTS (
					SELECT 1 FROM document_processing_requests
					WHERE id = ? AND workspace_id = ? AND document_id = ?
				) THEN 'NOT_FAILED'
				ELSE 'NOT_FOUND'
			END AS outcome
			""",
			(resultSet, rowNumber) -> DocumentProcessingRetryOutcome.valueOf(
				resultSet.getString("outcome")),
			requestId,
			workspaceId,
			documentId,
			requestId,
			workspaceId,
			documentId);
	}

	@Override
	public void markCompleted(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'COMPLETED', content = NULL, completed_at = now(), lease_until = NULL,
				last_error = NULL, updated_at = now()
			WHERE id = ? AND status = 'PROCESSING'
			""",
			requestId);
	}

	@Override
	public void releaseForRetry(UUID requestId, String error) {
		Objects.requireNonNull(requestId, "request id is required");
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'PUBLISHED', lease_until = NULL, last_error = ?, updated_at = now()
			WHERE id = ? AND status = 'PROCESSING'
			""",
			safeError(error, "Unknown processing failure"),
			requestId);
	}

	@Override
	public void markFailed(UUID requestId, String error) {
		Objects.requireNonNull(requestId, "request id is required");
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'FAILED', failed_at = now(), lease_until = NULL,
				last_error = ?, updated_at = now()
			WHERE id = ? AND status IN ('PENDING', 'DISPATCHING', 'PUBLISHED')
			""",
			safeError(error, "Unknown processing failure"),
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
					OR (status = 'PROCESSING' AND lease_until <= now())
				ORDER BY created_at, id
				FOR UPDATE SKIP LOCKED
				LIMIT ?
			)
			UPDATE document_processing_requests AS request
			SET status = 'DISPATCHING',
				dispatch_attempts = request.dispatch_attempts + 1,
				lease_until = now() + (CAST(? AS bigint) * interval '1 millisecond'),
				updated_at = now()
			FROM claimable
			WHERE request.id = claimable.id
			RETURNING request.id, request.dispatch_attempts
			""",
			(resultSet, rowNumber) -> new ClaimedDocumentProcessingRequest(
				resultSet.getObject("id", UUID.class),
				resultSet.getInt("dispatch_attempts")),
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
		jdbcTemplate.update(
			"""
			UPDATE document_processing_requests
			SET status = 'PENDING',
				available_at = now() + (CAST(? AS bigint) * interval '1 millisecond'),
				lease_until = NULL, last_error = ?, updated_at = now()
			WHERE id = ? AND status = 'DISPATCHING'
			""",
			delay.toMillis(),
			safeError(error, "Unknown publication failure"),
			requestId);
	}

	private static ProcessDocumentCommand toCommand(java.sql.ResultSet resultSet)
		throws java.sql.SQLException {
		return new ProcessDocumentCommand(
			resultSet.getObject("workspace_id", UUID.class),
			resultSet.getObject("document_id", UUID.class),
			resultSet.getString("content"),
			resultSet.getInt("max_tokens"));
	}

	private static Instant toInstant(java.sql.ResultSet resultSet, String column)
		throws java.sql.SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}

	private static Instant toNullableInstant(java.sql.ResultSet resultSet, String column)
		throws java.sql.SQLException {
		var timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static String safeError(String error, String defaultError) {
		var safeError = error == null || error.isBlank() ? defaultError : error;
		return safeError.length() > MAX_ERROR_LENGTH
			? safeError.substring(0, MAX_ERROR_LENGTH)
			: safeError;
	}

	private static void requirePositive(Duration duration, String message) {
		Objects.requireNonNull(duration, message);
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(message);
		}
	}

}
