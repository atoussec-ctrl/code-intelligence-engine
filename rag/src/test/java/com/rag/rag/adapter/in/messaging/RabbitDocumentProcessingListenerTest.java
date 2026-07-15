package com.rag.rag.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.usecase.ProcessDocumentUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RabbitDocumentProcessingListenerTest {

	@Test
	void processesReceivedMessage() {
		var processDocument = mock(ProcessDocumentUseCase.class);
		var listener = new RabbitDocumentProcessingListener(processDocument);
		var message = new DocumentProcessingMessage(UUID.randomUUID(), UUID.randomUUID(), "content", 256);

		listener.handle(message);

		verify(processDocument).execute(message.toCommand());
	}

	@Test
	void validatesDependency() {
		var thrown = assertThrows(
			NullPointerException.class,
			() -> new RabbitDocumentProcessingListener(null));

		assertEquals("process document is required", thrown.getMessage());
	}

}
