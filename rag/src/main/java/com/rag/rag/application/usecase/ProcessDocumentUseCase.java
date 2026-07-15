package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.ChunkEmbeddingRepositoryPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.service.ChunkingService;
import com.rag.rag.application.service.PromptInjectionScanner;
import com.rag.rag.domain.embedding.ChunkEmbedding;
import java.util.Objects;

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
		this.documents = Objects.requireNonNull(documents, "documents is required");
		this.chunkingService = Objects.requireNonNull(chunkingService, "chunking service is required");
		this.promptInjectionScanner = Objects.requireNonNull(
			promptInjectionScanner,
			"prompt injection scanner is required");
		this.embeddings = Objects.requireNonNull(embeddings, "embeddings are required");
		this.chunkEmbeddings = Objects.requireNonNull(chunkEmbeddings, "chunk embeddings are required");
	}

	public ProcessDocumentResult execute(ProcessDocumentCommand command) {
		Objects.requireNonNull(command, "command is required");
		var document = documents.findById(command.workspaceId(), command.documentId())
			.orElseThrow(DocumentNotFoundException::new);

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
			var failureReason = exception.getMessage();
			if (failureReason == null || failureReason.isBlank()) {
				failureReason = exception.getClass().getSimpleName();
			}
			documents.markFailed(command.workspaceId(), document.id(), failureReason);
			throw exception;
		}
	}

}
