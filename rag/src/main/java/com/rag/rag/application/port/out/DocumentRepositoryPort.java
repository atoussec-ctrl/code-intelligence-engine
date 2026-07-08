package com.rag.rag.application.port.out;

import com.rag.rag.domain.document.Document;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {

	Optional<Document> findById(UUID workspaceId, UUID documentId);

	void markProcessing(UUID workspaceId, UUID documentId);

	void markReady(UUID workspaceId, UUID documentId);

	void markFailed(UUID workspaceId, UUID documentId, String reason);

}