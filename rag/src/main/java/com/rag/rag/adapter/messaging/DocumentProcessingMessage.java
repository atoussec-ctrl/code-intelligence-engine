package com.rag.rag.adapter.messaging;

import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.util.Objects;
import java.util.UUID;

public record DocumentProcessingMessage(UUID workspaceId, UUID documentId, String content, int maxTokens) {

	public static DocumentProcessingMessage from(ProcessDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		return new DocumentProcessingMessage(
			command.workspaceId(),
			command.documentId(),
			command.content(),
			command.maxTokens());
	}

	public ProcessDocumentCommand toCommand() {
		return new ProcessDocumentCommand(workspaceId, documentId, content, maxTokens);
	}

}
