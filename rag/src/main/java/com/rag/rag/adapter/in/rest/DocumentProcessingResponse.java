package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestResult;
import java.time.Instant;
import java.util.UUID;

record DocumentProcessingResponse(
	UUID requestId,
	UUID workspaceId,
	UUID documentId,
	DocumentProcessingStatus status,
	boolean terminal,
	int dispatchAttempts,
	int processingAttempts,
	String lastError,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt,
	Instant completedAt,
	Instant failedAt) {

	static DocumentProcessingResponse from(GetDocumentProcessingRequestResult result) {
		return new DocumentProcessingResponse(
			result.requestId(),
			result.workspaceId(),
			result.documentId(),
			result.status(),
			result.status().isTerminal(),
			result.dispatchAttempts(),
			result.processingAttempts(),
			result.lastError(),
			result.createdAt(),
			result.updatedAt(),
			result.publishedAt(),
			result.completedAt(),
			result.failedAt());
	}

}
