package com.rag.rag.application.usecase;

import java.util.UUID;

public record GetDocumentQuery(UUID workspaceId, UUID documentId) {

	public GetDocumentQuery {
		if (workspaceId == null) {
			throw new IllegalArgumentException("workspace id is required");
		}
		if (documentId == null) {
			throw new IllegalArgumentException("document id is required");
		}
	}
}
