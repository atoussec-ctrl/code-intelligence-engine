package com.rag.rag.application.usecase;

import java.util.UUID;

public record RetryDocumentProcessingRequestCommand(
	UUID workspaceId,
	UUID documentId,
	UUID requestId) {

	public RetryDocumentProcessingRequestCommand {
		if (workspaceId == null) {
			throw new IllegalArgumentException("workspace id is required");
		}
		if (documentId == null) {
			throw new IllegalArgumentException("document id is required");
		}
		if (requestId == null) {
			throw new IllegalArgumentException("request id is required");
		}
	}

}
