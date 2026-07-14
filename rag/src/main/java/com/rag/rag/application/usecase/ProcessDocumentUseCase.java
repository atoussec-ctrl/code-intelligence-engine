package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.ChunkEmbeddingRepositoryPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.service.ChunkingService;
import com.rag.rag.application.service.PromptInjectionScanner;
import com.rag.rag.domain.embedding.ChunkEmbedding;

public class ProcessDocumentUseCase {

	private final DocumentRepositoryPort documents;
	private final ChunkingService chunkingService;
	private final PromptInjectionScanner promptInjectionScanner;
	private final EmbeddingGeneratorPort embeddings;
	private final ChunkEmbeddingRepositoryPort chunkEmbeddings;

	public ProcessDocumentUseCase(
		DocumentRepositoryPort documents,
		ChunkingService chunkingService,
		PromptInjectionScanner promptInjectionScanner,
		EmbeddingGeneratorPort embeddings,
		ChunkEmbeddingRepositoryPort chunkEmbeddings) {
		this.documents = documents;
		this.chunkingService = chunkingService;
		this.promptInjectionScanner = promptInjectionScanner;
		this.embeddings = embeddings;
		this.chunkEmbeddings = chunkEmbeddings;
	}

	public ProcessDocumentResult execute(ProcessDocumentCommand command) {
		var document = documents.findById(command.workspaceId(), command.documentId())
			.orElseThrow(() -> new IllegalArgumentException("document not found"));

		documents.markProcessing(command.workspaceId(), command.documentId());

		try {
			var chunks = chunkingService.split(
				command.workspaceId(),
				document.id(),
				command.content(),
				command.maxTokens(),
				document.metadata()).stream()
				.map(chunk -> chunk.withMetadata(promptInjectionScanner.scan(chunk.content())))
				.toList();
			var vectors = embeddings.generateBatch(chunks.stream().map(chunk -> chunk.content()).toList());

			if (vectors.size() != chunks.size()) {
				throw new IllegalStateException("embedding count does not match chunk count");
			}

			var persisted = java.util.stream.IntStream.range(0, chunks.size())
				.mapToObj(index -> ChunkEmbedding.of(chunks.get(index), vectors.get(index)))
				.toList();

			chunkEmbeddings.saveAll(command.workspaceId(), document.id(), persisted);
			documents.markReady(command.workspaceId(), document.id());

			return new ProcessDocumentResult(document.id(), chunks.size());
		}
		catch (RuntimeException exception) {
			documents.markFailed(command.workspaceId(), document.id(), exception.getMessage());
			throw exception;
		}
	}

}
