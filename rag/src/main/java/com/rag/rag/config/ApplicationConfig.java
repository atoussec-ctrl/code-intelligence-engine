package com.rag.rag.config;

import com.rag.rag.adapter.out.embedding.SpringAiEmbeddingGeneratorAdapter;
import com.rag.rag.application.port.out.ChunkEmbeddingRepositoryPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.application.port.out.DocumentProcessingQueuePort;
import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.service.ChunkingService;
import com.rag.rag.application.service.PromptInjectionScanner;
import com.rag.rag.application.usecase.GetDocumentUseCase;
import com.rag.rag.application.usecase.GetHealthStatusUseCase;
import com.rag.rag.application.usecase.ProcessDocumentUseCase;
import com.rag.rag.application.usecase.RegisterDocumentUseCase;
import com.rag.rag.application.usecase.RequestDocumentProcessingUseCase;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

	@Bean
	GetHealthStatusUseCase getHealthStatusUseCase() {
		return new GetHealthStatusUseCase();
	}

	@Bean
	PromptInjectionScanner promptInjectionScanner() {
		return new PromptInjectionScanner();
	}

	@Bean
	ChunkingService chunkingService() {
		return new ChunkingService();
	}

	@Bean
	EmbeddingGeneratorPort embeddingGeneratorPort(
		EmbeddingModel embeddingModel,
		@Value("${spring.ai.ollama.embedding.model:mxbai-embed-large}") String modelName) {
		return new SpringAiEmbeddingGeneratorAdapter(embeddingModel, modelName);
	}

	@Bean
	RegisterDocumentUseCase registerDocumentUseCase(DocumentRepositoryPort documents) {
		return new RegisterDocumentUseCase(documents);
	}

	@Bean
	GetDocumentUseCase getDocumentUseCase(DocumentRepositoryPort documents) {
		return new GetDocumentUseCase(documents);
	}

	@Bean
	RequestDocumentProcessingUseCase requestDocumentProcessingUseCase(
		DocumentRepositoryPort documents,
		DocumentProcessingQueuePort processingQueue) {
		return new RequestDocumentProcessingUseCase(documents, processingQueue);
	}

	@Bean
	ProcessDocumentUseCase processDocumentUseCase(
		DocumentRepositoryPort documents,
		ChunkingService chunkingService,
		PromptInjectionScanner promptInjectionScanner,
		EmbeddingGeneratorPort embeddings,
		ChunkEmbeddingRepositoryPort chunkEmbeddings) {
		return new ProcessDocumentUseCase(
			documents,
			chunkingService,
			promptInjectionScanner,
			embeddings,
			chunkEmbeddings);
	}

}
