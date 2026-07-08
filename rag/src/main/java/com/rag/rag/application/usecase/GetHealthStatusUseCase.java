package com.rag.rag.application.usecase;

public class GetHealthStatusUseCase {

	public HealthStatus execute() {
		return new HealthStatus("rag-engine-java", "UP");
	}

}