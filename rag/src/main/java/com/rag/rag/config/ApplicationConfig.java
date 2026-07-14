package com.rag.rag.config;

import com.rag.rag.application.service.PromptInjectionScanner;
import com.rag.rag.application.usecase.GetHealthStatusUseCase;
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

}
