package com.rag.rag.application.usecase;

import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.Map;
import java.util.UUID;

public record GetDocumentResult(
	UUID documentId,
	UUID workspaceId,
	String title,
	DocumentSourceType sourceType,
	String sourceUri,
	String checksum,
	DocumentStatus status,
	Map<String, String> metadata) {

	public GetDocumentResult {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}

	static GetDocumentResult from(Document document) {
		return new GetDocumentResult(
			document.id(),
			document.workspaceId(),
			document.title(),
			document.source().type(),
			document.source().uri(),
			document.checksum(),
			document.status(),
			document.metadata());
	}
}
