package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.ChunkEmbeddingRepositoryPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.service.ChunkingService;
import com.rag.rag.domain.document.Document;
import com.rag.rag.domain.document.DocumentSource;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import com.rag.rag.domain.embedding.EmbeddingVector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDocumentUseCaseTest {

	@Test
	void processesDocumentWithFakePorts() {
		var workspaceId = UUID.randomUUID();
		var document = Document.create(
			workspaceId,
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of("tag", "architecture"));
		var documents = new FakeDocumentRepository(document);
		var embeddings = new FakeEmbeddingGenerator();
		var chunkEmbeddings = new FakeChunkEmbeddingRepository();
		var useCase = new ProcessDocumentUseCase(
			documents,
			new ChunkingService(),
			embeddings,
			chunkEmbeddings);

		var result = useCase.execute(new ProcessDocumentCommand(
			workspaceId,
			document.id(),
			"one two three four five",
			2));

		assertEquals(document.id(), result.documentId());
		assertEquals(3, result.chunksCreated());
		assertEquals(List.of("PROCESSING", "READY"), documents.statusTransitions);
		assertEquals(List.of("one two", "three four", "five"), embeddings.requestedTexts);
		assertEquals(3, chunkEmbeddings.saved.size());
		assertTrue(chunkEmbeddings.saved.stream().allMatch(item -> workspaceId.equals(item.chunk().workspaceId())));
		assertTrue(chunkEmbeddings.saved.stream().allMatch(item -> document.id().equals(item.chunk().documentId())));
	}

	@Test
	void failsWhenDocumentDoesNotExistWithoutGeneratingEmbeddings() {
		var documents = new FakeDocumentRepository(null);
		var embeddings = new FakeEmbeddingGenerator();
		var chunkEmbeddings = new FakeChunkEmbeddingRepository();
		var useCase = new ProcessDocumentUseCase(
			documents,
			new ChunkingService(),
			embeddings,
			chunkEmbeddings);

		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> useCase.execute(new ProcessDocumentCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"content",
				2)));

		assertEquals("document not found", thrown.getMessage());
		assertTrue(embeddings.requestedTexts.isEmpty());
		assertTrue(chunkEmbeddings.saved.isEmpty());
	}

	@Test
	void marksDocumentAsFailedWhenEmbeddingGenerationFails() {
		var workspaceId = UUID.randomUUID();
		var document = Document.create(
			workspaceId,
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of());
		var documents = new FakeDocumentRepository(document);
		var embeddings = new FakeEmbeddingGenerator();
		embeddings.fail = true;
		var chunkEmbeddings = new FakeChunkEmbeddingRepository();
		var useCase = new ProcessDocumentUseCase(
			documents,
			new ChunkingService(),
			embeddings,
			chunkEmbeddings);

		var thrown = assertThrows(
			IllegalStateException.class,
			() -> useCase.execute(new ProcessDocumentCommand(
				workspaceId,
				document.id(),
				"one two",
				2)));

		assertEquals("embedding provider unavailable", thrown.getMessage());
		assertEquals(List.of("PROCESSING", "FAILED:embedding provider unavailable"), documents.statusTransitions);
		assertTrue(chunkEmbeddings.saved.isEmpty());
	}

	private static final class FakeDocumentRepository implements DocumentRepositoryPort {

		private final Document document;
		private final List<String> statusTransitions = new ArrayList<>();

		private FakeDocumentRepository(Document document) {
			this.document = document;
		}

		@Override
		public Optional<Document> findById(UUID workspaceId, UUID documentId) {
			if (document == null) {
				return Optional.empty();
			}
			if (!document.workspaceId().equals(workspaceId) || !document.id().equals(documentId)) {
				return Optional.empty();
			}
			return Optional.of(document);
		}

		@Override
		public void markProcessing(UUID workspaceId, UUID documentId) {
			statusTransitions.add("PROCESSING");
		}

		@Override
		public void markReady(UUID workspaceId, UUID documentId) {
			statusTransitions.add("READY");
		}

		@Override
		public void markFailed(UUID workspaceId, UUID documentId, String reason) {
			statusTransitions.add("FAILED:" + reason);
		}
	}

	private static final class FakeEmbeddingGenerator implements EmbeddingGeneratorPort {

		private final List<String> requestedTexts = new ArrayList<>();
		private boolean fail;

		@Override
		public List<EmbeddingVector> generateBatch(List<String> texts) {
			requestedTexts.addAll(texts);
			if (fail) {
				throw new IllegalStateException("embedding provider unavailable");
			}
			return texts.stream()
				.map(text -> EmbeddingVector.of(List.of(0.1, 0.2, (double) text.length()), "fake-embedding-model"))
				.toList();
		}
	}

	private static final class FakeChunkEmbeddingRepository implements ChunkEmbeddingRepositoryPort {

		private final List<ChunkEmbedding> saved = new ArrayList<>();

		@Override
		public void saveAll(UUID workspaceId, UUID documentId, List<ChunkEmbedding> chunks) {
			saved.addAll(chunks);
		}
	}

}