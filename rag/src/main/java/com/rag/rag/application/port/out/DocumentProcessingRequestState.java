package com.rag.rag.application.port.out;

import com.rag.rag.application.usecase.DocumentProcessingStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DocumentProcessingRequestState(
	UUID requestId,
	UUID workspaceId,
	UUID documentId,
	DocumentProcessingStatus status,
	int dispatchAttempts,
	int processingAttempts,
	String lastError,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt,
	Instant completedAt,
	Instant failedAt) {

	public DocumentProcessingRequestState {
		Objects.requireNonNull(requestId, "request id is required");
		Objects.requireNonNull(workspaceId, "workspace id is required");
		Objects.requireNonNull(documentId, "document id is required");
		Objects.requireNonNull(status, "status is required");
		Objects.requireNonNull(createdAt, "created at is required");
		Objects.requireNonNull(updatedAt, "updated at is required");
		if (dispatchAttempts < 0 || processingAttempts < 0) {
			throw new IllegalArgumentException("attempt counts must not be negative");
		}
	}

}
