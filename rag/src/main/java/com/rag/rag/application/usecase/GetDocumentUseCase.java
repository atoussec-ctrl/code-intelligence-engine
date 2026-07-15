package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import java.util.Objects;

public class GetDocumentUseCase {

	private final DocumentRepositoryPort documents;

	public GetDocumentUseCase(DocumentRepositoryPort documents) {
		this.documents = Objects.requireNonNull(documents, "documents is required");
	}

	public GetDocumentResult execute(GetDocumentQuery query) {
		Objects.requireNonNull(query, "query is required");
		var document = documents.findById(query.workspaceId(), query.documentId())
			.orElseThrow(DocumentNotFoundException::new);
		return GetDocumentResult.from(document);
	}
}
