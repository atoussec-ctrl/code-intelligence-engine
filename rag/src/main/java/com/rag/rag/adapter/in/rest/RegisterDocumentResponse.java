package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.RegisterDocumentResult;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.UUID;

record RegisterDocumentResponse(UUID documentId, DocumentStatus status) {

	static RegisterDocumentResponse from(RegisterDocumentResult result) {
		return new RegisterDocumentResponse(result.documentId(), result.status());
	}
}
