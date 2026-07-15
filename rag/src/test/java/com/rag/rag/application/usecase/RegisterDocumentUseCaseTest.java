package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterDocumentUseCaseTest {

	@Test
	void registersDocumentWithInitialIngestionStatus() {
		var documents = new FakeDocumentRepository();
		var useCase = new RegisterDocumentUseCase(documents);
		var workspaceId = UUID.randomUUID();

		var result = useCase.execute(new RegisterDocumentCommand(
			workspaceId,
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of("tag", "architecture")));

		assertNotNull(result.documentId());
		assertEquals(DocumentStatus.INGESTION_REQUESTED, result.status());
		assertEquals(result.documentId(), documents.saved.id());
		assertEquals(workspaceId, documents.saved.workspaceId());
		assertEquals("Architecture Notes", documents.saved.title());
		assertEquals("checksum-123", documents.saved.checksum());
		assertEquals("architecture", documents.saved.metadata().get("tag"));
	}

	@Test
	void requiresRepository() {
		var thrown = assertThrows(
			NullPointerException.class,
			() -> new RegisterDocumentUseCase(null));

		assertEquals("documents is required", thrown.getMessage());
	}

	@Test
	void requiresCommand() {
		var useCase = new RegisterDocumentUseCase(new FakeDocumentRepository());

		var thrown = assertThrows(
			NullPointerException.class,
			() -> useCase.execute(null));

		assertEquals("command is required", thrown.getMessage());
	}

	private static final class FakeDocumentRepository implements DocumentRepositoryPort {

		private Document saved;

		@Override
		public Document save(Document document) {
			saved = document;
			return document;
		}

		@Override
		public Optional<Document> findById(UUID workspaceId, UUID documentId) {
			return Optional.empty();
		}

		@Override
		public void markProcessing(UUID workspaceId, UUID documentId) {
		}

		@Override
		public void markReady(UUID workspaceId, UUID documentId) {
		}

		@Override
		public void markFailed(UUID workspaceId, UUID documentId, String reason) {
		}
	}
}
