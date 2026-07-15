package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.util.Objects;
import java.util.UUID;

public class ProcessQueuedDocumentUseCase {

	private final DocumentProcessingRequestPort processingRequests;
	private final ProcessDocumentUseCase processDocument;

	public ProcessQueuedDocumentUseCase(
		DocumentProcessingRequestPort processingRequests,
		ProcessDocumentUseCase processDocument) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
		this.processDocument = Objects.requireNonNull(processDocument, "process document is required");
	}

	public ProcessDocumentResult execute(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		var command = processingRequests.findCommandById(requestId)
			.orElseThrow(DocumentProcessingRequestNotFoundException::new);
		var result = processDocument.execute(command);
		processingRequests.markCompleted(requestId);
		return result;
	}

}
