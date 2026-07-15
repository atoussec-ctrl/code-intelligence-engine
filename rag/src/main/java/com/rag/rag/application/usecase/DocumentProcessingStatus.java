package com.rag.rag.application.usecase;

public enum DocumentProcessingStatus {

	PENDING,
	DISPATCHING,
	PUBLISHED,
	PROCESSING,
	COMPLETED,
	FAILED;

	public boolean isTerminal() {
		return this == COMPLETED || this == FAILED;
	}

}
