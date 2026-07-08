package com.rag.rag.application.usecase;

import java.util.UUID;

public record ProcessDocumentCommand(UUID workspaceId, UUID documentId, String content, int maxTokens) {

	public ProcessDocumentCommand {
		if (workspaceId == null) {
			throw new IllegalArgumentException("workspace id is required");
		}
		if (documentId == null) {
			throw new IllegalArgumentException("document id is required");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content is required");
		}
		if (maxTokens <= 0) {
			throw new IllegalArgumentException("max tokens must be positive");
		}
	}

}