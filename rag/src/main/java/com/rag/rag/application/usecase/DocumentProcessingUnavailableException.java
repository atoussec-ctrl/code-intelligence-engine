package com.rag.rag.application.usecase;

public class DocumentProcessingUnavailableException extends RuntimeException {

	public DocumentProcessingUnavailableException(Throwable cause) {
		super("document processing is temporarily unavailable", cause);
	}

}
