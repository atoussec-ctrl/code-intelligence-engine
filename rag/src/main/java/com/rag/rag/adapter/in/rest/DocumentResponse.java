package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.GetDocumentResult;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.Map;
import java.util.UUID;

record DocumentResponse(
	UUID documentId,
	UUID workspaceId,
	String title,
	DocumentSourceType sourceType,
	String sourceUri,
	String checksum,
	DocumentStatus status,
	Map<String, String> metadata) {

	static DocumentResponse from(GetDocumentResult result) {
		return new DocumentResponse(
			result.documentId(),
			result.workspaceId(),
			result.title(),
			result.sourceType(),
			result.sourceUri(),
			result.checksum(),
			result.status(),
			result.metadata());
	}
}
