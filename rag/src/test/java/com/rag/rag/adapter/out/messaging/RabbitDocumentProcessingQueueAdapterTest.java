package com.rag.rag.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.usecase.DocumentProcessingUnavailableException;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitDocumentProcessingQueueAdapterTest {

	@Test
	void publishesDocumentProcessingMessage() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var adapter = new RabbitDocumentProcessingQueueAdapter(rabbitTemplate);
		var command = command();

		adapter.enqueue(command);

		verify(rabbitTemplate).convertAndSend(
			eq(RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE),
			eq(RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY),
			eq(DocumentProcessingMessage.from(command)));
	}

	@Test
	void translatesBrokerFailure() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var adapter = new RabbitDocumentProcessingQueueAdapter(rabbitTemplate);
		var command = command();
		var cause = new AmqpException("broker unavailable");
		doThrow(cause).when(rabbitTemplate).convertAndSend(
			RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE,
			RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY,
			DocumentProcessingMessage.from(command));

		var thrown = assertThrows(DocumentProcessingUnavailableException.class, () -> adapter.enqueue(command));

		assertEquals("document processing is temporarily unavailable", thrown.getMessage());
		assertSame(cause, thrown.getCause());
	}

	@Test
	void validatesDependency() {
		var thrown = assertThrows(
			NullPointerException.class,
			() -> new RabbitDocumentProcessingQueueAdapter(null));

		assertEquals("rabbit template is required", thrown.getMessage());
	}

	private ProcessDocumentCommand command() {
		return new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);
	}

}
