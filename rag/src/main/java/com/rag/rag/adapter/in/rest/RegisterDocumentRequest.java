package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.RegisterDocumentCommand;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentSourceType;
import java.util.Map;
import java.util.UUID;

record RegisterDocumentRequest(
	String title,
	DocumentSourceType sourceType,
	String sourceUri,
	String checksum,
	Map<String, String> metadata) {

	RegisterDocumentCommand toCommand(UUID workspaceId) {
		return new RegisterDocumentCommand(
			workspaceId,
			title,
			toSource(),
			checksum,
			metadata);
	}

	private DocumentSource toSource() {
		if (sourceType == null) {
			throw new IllegalArgumentException("source type is required");
		}
		return switch (sourceType) {
			case TEXT -> DocumentSource.text();
			case URL -> DocumentSource.url(sourceUri);
			case GITHUB -> DocumentSource.github(sourceUri);
			case FILE -> DocumentSource.file(sourceUri);
		};
	}
}
