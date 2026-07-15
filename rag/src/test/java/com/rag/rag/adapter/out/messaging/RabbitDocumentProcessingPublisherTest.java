package com.rag.rag.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitDocumentProcessingPublisherTest {

	@Test
	void publishesReferenceAndWaitsForBrokerConfirmation() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var requestId = UUID.randomUUID();
		completeConfirmation(rabbitTemplate, true, null, false);
		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofSeconds(1));

		publisher.publish(requestId);

		var correlation = ArgumentCaptor.forClass(CorrelationData.class);
		verify(rabbitTemplate).convertAndSend(
			eq(RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE),
			eq(RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY),
			eq(new DocumentProcessingMessage(requestId)),
			correlation.capture());
		assertEquals(requestId.toString(), correlation.getValue().getId());
	}

	@Test
	void rejectsNegativeConfirmation() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		completeConfirmation(rabbitTemplate, false, "broker nack", false);
		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofSeconds(1));

		var thrown = assertThrows(
			DocumentProcessingPublishException.class,
			() -> publisher.publish(UUID.randomUUID()));

		assertEquals("document processing message was not confirmed: broker nack", thrown.getMessage());
	}

	@Test
	void rejectsReturnedMessageEvenWhenBrokerAcknowledgesPublish() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		completeConfirmation(rabbitTemplate, true, null, true);
		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofSeconds(1));

		var thrown = assertThrows(
			DocumentProcessingPublishException.class,
			() -> publisher.publish(UUID.randomUUID()));

		assertEquals("document processing message was not routed", thrown.getMessage());
	}

	@Test
	void translatesImmediateAmqpFailure() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var cause = new AmqpException("connection refused");
		doThrow(cause).when(rabbitTemplate).convertAndSend(
			any(String.class),
			any(String.class),
			any(DocumentProcessingMessage.class),
			any(CorrelationData.class));
		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofSeconds(1));

		var thrown = assertThrows(
			DocumentProcessingPublishException.class,
			() -> publisher.publish(UUID.randomUUID()));

		assertEquals("document processing publication failed", thrown.getMessage());
		assertSame(cause, thrown.getCause());
	}

	@Test
	void failsWhenConfirmationTimesOut() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofMillis(1));

		var thrown = assertThrows(
			DocumentProcessingPublishException.class,
			() -> publisher.publish(UUID.randomUUID()));

		assertEquals("document processing publication failed", thrown.getMessage());
		assertTrue(thrown.getCause() instanceof java.util.concurrent.TimeoutException);
	}

	@Test
	void preservesInterruptStatus() {
		var publisher = new RabbitDocumentProcessingPublisher(mock(RabbitTemplate.class), Duration.ofSeconds(1));
		Thread.currentThread().interrupt();
		try {
			var thrown = assertThrows(
				DocumentProcessingPublishException.class,
				() -> publisher.publish(UUID.randomUUID()));

			assertEquals("document processing publication was interrupted", thrown.getMessage());
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally {
			assertTrue(Thread.interrupted());
			assertFalse(Thread.currentThread().isInterrupted());
		}
	}

	@Test
	void validatesArguments() {
		var rabbitTemplate = mock(RabbitTemplate.class);

		assertEquals(
			"rabbit template is required",
			assertThrows(
				NullPointerException.class,
				() -> new RabbitDocumentProcessingPublisher(null, Duration.ofSeconds(1))).getMessage());
		assertEquals(
			"confirm timeout is required",
			assertThrows(
				NullPointerException.class,
				() -> new RabbitDocumentProcessingPublisher(rabbitTemplate, null)).getMessage());
		assertEquals(
			"confirm timeout must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ZERO)).getMessage());

		var publisher = new RabbitDocumentProcessingPublisher(rabbitTemplate, Duration.ofSeconds(1));
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> publisher.publish(null)).getMessage());
	}

	private void completeConfirmation(
		RabbitTemplate rabbitTemplate,
		boolean acknowledged,
		String reason,
		boolean returned) {
		doAnswer(invocation -> {
			var correlation = invocation.getArgument(3, CorrelationData.class);
			if (returned) {
				correlation.setReturned(new ReturnedMessage(
					new Message(new byte[0]),
					312,
					"NO_ROUTE",
					RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE,
					RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY));
			}
			correlation.getFuture().complete(new CorrelationData.Confirm(acknowledged, reason));
			return null;
		}).when(rabbitTemplate).convertAndSend(
			any(String.class),
			any(String.class),
			any(DocumentProcessingMessage.class),
			any(CorrelationData.class));
	}

}
