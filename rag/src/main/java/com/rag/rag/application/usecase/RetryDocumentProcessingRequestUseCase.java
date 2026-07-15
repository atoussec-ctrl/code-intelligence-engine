package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentProcessingRetryOutcome;
import java.util.Objects;

public class RetryDocumentProcessingRequestUseCase {

	private final DocumentProcessingRequestPort processingRequests;

	public RetryDocumentProcessingRequestUseCase(DocumentProcessingRequestPort processingRequests) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
	}

	public void execute(RetryDocumentProcessingRequestCommand command) {
		Objects.requireNonNull(command, "command is required");
		var outcome = Objects.requireNonNull(
			processingRequests.retryFailed(
				command.workspaceId(),
				command.documentId(),
				command.requestId()),
			"retry outcome is required");
		if (outcome == DocumentProcessingRetryOutcome.NOT_FOUND) {
			throw new DocumentProcessingRequestNotFoundException();
		}
		if (outcome == DocumentProcessingRetryOutcome.NOT_FAILED) {
			throw new DocumentProcessingRequestNotRetryableException();
		}
	}

}
