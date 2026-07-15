package com.rag.rag.config;

import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.application.service.PromptInjectionScanner;
import com.rag.rag.application.usecase.GetDocumentUseCase;
import com.rag.rag.application.usecase.GetHealthStatusUseCase;
import com.rag.rag.application.usecase.RegisterDocumentUseCase;
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
	RegisterDocumentUseCase registerDocumentUseCase(DocumentRepositoryPort documents) {
		return new RegisterDocumentUseCase(documents);
	}

	@Bean
	GetDocumentUseCase getDocumentUseCase(DocumentRepositoryPort documents) {
		return new GetDocumentUseCase(documents);
	}

}
