package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.util.Objects;

public class GetDocumentProcessingRequestUseCase {

	private final DocumentProcessingRequestPort processingRequests;

	public GetDocumentProcessingRequestUseCase(DocumentProcessingRequestPort processingRequests) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
	}

	public GetDocumentProcessingRequestResult execute(GetDocumentProcessingRequestQuery query) {
		Objects.requireNonNull(query, "query is required");
		return processingRequests.findById(query.workspaceId(), query.documentId(), query.requestId())
			.map(GetDocumentProcessingRequestResult::from)
			.orElseThrow(DocumentProcessingRequestNotFoundException::new);
	}

}
