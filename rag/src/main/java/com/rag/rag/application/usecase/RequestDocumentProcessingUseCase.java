package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import java.util.Objects;
import java.util.UUID;

public class RequestDocumentProcessingUseCase {

	private final DocumentRepositoryPort documents;
	private final DocumentProcessingRequestPort processingRequests;

	public RequestDocumentProcessingUseCase(
		DocumentRepositoryPort documents,
		DocumentProcessingRequestPort processingRequests) {
		this.documents = Objects.requireNonNull(documents, "documents is required");
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
	}

	public UUID execute(ProcessDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		documents.findById(command.workspaceId(), command.documentId())
			.orElseThrow(DocumentNotFoundException::new);
		return processingRequests.create(command);
	}

}
