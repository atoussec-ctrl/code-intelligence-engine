package com.rag.rag.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;

class DocumentProcessingMessageRecovererTest {

	@Test
	void marksRequestFailedAndRejectsMessageAfterRetriesAreExhausted() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var converter = mock(MessageConverter.class);
		var message = new Message(new byte[0]);
		var requestId = UUID.randomUUID();
		when(converter.fromMessage(message)).thenReturn(new DocumentProcessingMessage(requestId));
		var recoverer = new DocumentProcessingMessageRecoverer(requests, converter);
		var cause = new IllegalStateException("listener failed", new RuntimeException("ollama unavailable"));

		assertThrows(
			AmqpRejectAndDontRequeueException.class,
			() -> recoverer.recover(message, cause));

		verify(requests).markFailed(requestId, "ollama unavailable");
	}

	@Test
	void stillRejectsMalformedMessageWithoutChangingARequest() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var converter = mock(MessageConverter.class);
		var message = new Message(new byte[0]);
		when(converter.fromMessage(message)).thenReturn("unsupported payload");
		var recoverer = new DocumentProcessingMessageRecoverer(requests, converter);

		assertThrows(
			AmqpRejectAndDontRequeueException.class,
			() -> recoverer.recover(message, new IllegalStateException()));

		verifyNoInteractions(requests);
	}

	@Test
	void validatesDependenciesAndRecoveryArguments() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var converter = mock(MessageConverter.class);
		var recoverer = new DocumentProcessingMessageRecoverer(requests, converter);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new DocumentProcessingMessageRecoverer(null, converter)).getMessage());
		assertEquals(
			"message converter is required",
			assertThrows(
				NullPointerException.class,
				() -> new DocumentProcessingMessageRecoverer(requests, null)).getMessage());
		assertEquals(
			"message is required",
			assertThrows(
				NullPointerException.class,
				() -> recoverer.recover(null, new IllegalStateException())).getMessage());
		assertEquals(
			"cause is required",
			assertThrows(
				NullPointerException.class,
				() -> recoverer.recover(new Message(new byte[0]), null)).getMessage());
	}

}
