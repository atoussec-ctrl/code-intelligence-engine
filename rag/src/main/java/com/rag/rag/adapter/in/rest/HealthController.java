package com.rag.rag.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rag.rag.application.usecase.GetHealthStatusUseCase;
import com.rag.rag.application.usecase.HealthStatus;

@RestController
class HealthController {

	private final GetHealthStatusUseCase getHealthStatus;

	HealthController(GetHealthStatusUseCase getHealthStatus) {
		this.getHealthStatus = getHealthStatus;
	}

	@GetMapping("/health")
	HealthStatus getHealth() {
		return getHealthStatus.execute();
	}

}