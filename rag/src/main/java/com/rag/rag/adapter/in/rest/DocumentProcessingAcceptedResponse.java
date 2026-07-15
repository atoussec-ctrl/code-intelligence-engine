package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.DocumentProcessingStatus;
import java.util.UUID;

record DocumentProcessingAcceptedResponse(UUID requestId, DocumentProcessingStatus status) {

	static DocumentProcessingAcceptedResponse pending(UUID requestId) {
		return new DocumentProcessingAcceptedResponse(requestId, DocumentProcessingStatus.PENDING);
	}

}
