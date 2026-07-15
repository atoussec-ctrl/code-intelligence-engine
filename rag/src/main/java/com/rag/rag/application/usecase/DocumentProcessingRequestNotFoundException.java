package com.rag.rag.application.usecase;

public class DocumentProcessingRequestNotFoundException extends RuntimeException {

	public DocumentProcessingRequestNotFoundException() {
		super("document processing request not found");
	}

}
