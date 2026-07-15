package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.util.UUID;

record ProcessDocumentRequest(String content, int maxTokens) {

	ProcessDocumentCommand toCommand(UUID workspaceId, UUID documentId) {
		return new ProcessDocumentCommand(workspaceId, documentId, content, maxTokens);
	}

}
