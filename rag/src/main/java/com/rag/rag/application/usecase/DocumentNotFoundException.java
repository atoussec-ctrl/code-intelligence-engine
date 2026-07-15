package com.rag.rag.application.usecase;

public final class DocumentNotFoundException extends RuntimeException {

	public DocumentNotFoundException() {
		super("document not found");
	}
}
