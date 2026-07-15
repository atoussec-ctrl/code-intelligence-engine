package com.rag.rag.application.usecase;

public class DocumentProcessingRequestNotRetryableException extends RuntimeException {

	public DocumentProcessingRequestNotRetryableException() {
		super("only failed document processing requests can be retried");
	}

}
