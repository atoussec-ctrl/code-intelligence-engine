package com.rag.rag.adapter.messaging;

import java.util.Objects;
import java.util.UUID;

public record DocumentProcessingMessage(UUID requestId) {

	public DocumentProcessingMessage {
		Objects.requireNonNull(requestId, "request id is required");
	}

}
