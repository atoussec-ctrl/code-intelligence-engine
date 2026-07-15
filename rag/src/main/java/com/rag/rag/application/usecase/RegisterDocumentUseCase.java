package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import java.util.Objects;

public class RegisterDocumentUseCase {

	private final DocumentRepositoryPort documents;

	public RegisterDocumentUseCase(DocumentRepositoryPort documents) {
		this.documents = Objects.requireNonNull(documents, "documents is required");
	}

	public RegisterDocumentResult execute(RegisterDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		var document = Document.create(
			command.workspaceId(),
			command.title(),
			command.source(),
			command.checksum(),
			command.metadata());

		var saved = documents.save(document);
		return new RegisterDocumentResult(saved.id(), saved.status());
	}
}
