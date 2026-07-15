package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingQueuePort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import java.util.Objects;

public class RequestDocumentProcessingUseCase {

	private final DocumentRepositoryPort documents;
	private final DocumentProcessingQueuePort processingQueue;

	public RequestDocumentProcessingUseCase(
		DocumentRepositoryPort documents,
		DocumentProcessingQueuePort processingQueue) {
		this.documents = Objects.requireNonNull(documents, "documents is required");
		this.processingQueue = Objects.requireNonNull(processingQueue, "processing queue is required");
	}

	public void execute(ProcessDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		documents.findById(command.workspaceId(), command.documentId())
			.orElseThrow(DocumentNotFoundException::new);
		processingQueue.enqueue(command);
	}

}
