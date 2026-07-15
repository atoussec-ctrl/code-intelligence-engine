package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestState;
import java.time.Instant;
import java.util.UUID;

public record GetDocumentProcessingRequestResult(
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

	static GetDocumentProcessingRequestResult from(DocumentProcessingRequestState state) {
		return new GetDocumentProcessingRequestResult(
			state.requestId(),
			state.workspaceId(),
			state.documentId(),
			state.status(),
			state.dispatchAttempts(),
			state.processingAttempts(),
			state.lastError(),
			state.createdAt(),
			state.updatedAt(),
			state.publishedAt(),
			state.completedAt(),
			state.failedAt());
	}

}
