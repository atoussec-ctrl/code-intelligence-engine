package com.rag.rag.application.port.out;

import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.util.Objects;

public record DocumentProcessingClaim(
	DocumentProcessingStatus status,
	ProcessDocumentCommand command) {

	public DocumentProcessingClaim {
		Objects.requireNonNull(status, "status is required");
		if (command != null && status != DocumentProcessingStatus.PROCESSING) {
			throw new IllegalArgumentException("an acquired claim must be processing");
		}
	}

	public static DocumentProcessingClaim acquired(ProcessDocumentCommand command) {
		return new DocumentProcessingClaim(
			DocumentProcessingStatus.PROCESSING,
			Objects.requireNonNull(command, "command is required"));
	}

	public static DocumentProcessingClaim notAcquired(DocumentProcessingStatus status) {
		return new DocumentProcessingClaim(status, null);
	}

	public boolean acquired() {
		return command != null;
	}

}
