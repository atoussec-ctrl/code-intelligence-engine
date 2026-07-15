package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentUseCaseTest {

	@Test
	void returnsDocumentWithinWorkspaceScope() {
		var workspaceId = UUID.randomUUID();
		var document = Document.create(
			workspaceId,
			"Architecture Notes",
			DocumentSource.url("https://example.com/docs"),
			"checksum-123",
			Map.of("tag", "architecture"));
		var useCase = new GetDocumentUseCase(new FakeDocumentRepository(document));

		var result = useCase.execute(new GetDocumentQuery(workspaceId, document.id()));

		assertEquals(document.id(), result.documentId());
		assertEquals(workspaceId, result.workspaceId());
		assertEquals("Architecture Notes", result.title());
		assertEquals(DocumentSourceType.URL, result.sourceType());
		assertEquals("https://example.com/docs", result.sourceUri());
		assertEquals("checksum-123", result.checksum());
		assertEquals(DocumentStatus.INGESTION_REQUESTED, result.status());
		assertEquals("architecture", result.metadata().get("tag"));
	}

	@Test
	void reportsDocumentNotFoundOutsideWorkspaceScope() {
		var document = Document.create(
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of());
		var useCase = new GetDocumentUseCase(new FakeDocumentRepository(document));

		var thrown = assertThrows(
			DocumentNotFoundException.class,
			() -> useCase.execute(new GetDocumentQuery(UUID.randomUUID(), document.id())));

		assertEquals("document not found", thrown.getMessage());
	}

	@Test
	void requiresRepository() {
		var thrown = assertThrows(
			NullPointerException.class,
			() -> new GetDocumentUseCase(null));

		assertEquals("documents is required", thrown.getMessage());
	}

	@Test
	void requiresQuery() {
		var useCase = new GetDocumentUseCase(new FakeDocumentRepository(null));

		var thrown = assertThrows(NullPointerException.class, () -> useCase.execute(null));

		assertEquals("query is required", thrown.getMessage());
	}

	private static final class FakeDocumentRepository implements DocumentRepositoryPort {

		private final Document document;

		private FakeDocumentRepository(Document document) {
			this.document = document;
		}

		@Override
		public Document save(Document document) {
			throw new UnsupportedOperationException("save is not used by this test");
		}

		@Override
		public Optional<Document> findById(UUID workspaceId, UUID documentId) {
			if (document == null
				|| !document.workspaceId().equals(workspaceId)
				|| !document.id().equals(documentId)) {
				return Optional.empty();
			}
			return Optional.of(document);
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
