package com.rag.rag.adapter.out.messaging;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitDocumentProcessingPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final Duration confirmTimeout;

	public RabbitDocumentProcessingPublisher(
		RabbitTemplate rabbitTemplate,
		@Value("${rag.document-processing.publisher-confirm-timeout:5s}") Duration confirmTimeout) {
		this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbit template is required");
		this.confirmTimeout = requirePositive(confirmTimeout);
	}

	public void publish(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		var correlation = new CorrelationData(requestId.toString());
		try {
			rabbitTemplate.convertAndSend(
				RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE,
				RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY,
				new DocumentProcessingMessage(requestId),
				correlation);
			var confirmation = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (correlation.getReturned() != null) {
				throw new DocumentProcessingPublishException("document processing message was not routed");
			}
			if (!confirmation.ack()) {
				throw new DocumentProcessingPublishException(
					"document processing message was not confirmed: " + confirmation.reason());
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new DocumentProcessingPublishException("document processing publication was interrupted", exception);
		}
		catch (AmqpException | ExecutionException | TimeoutException exception) {
			throw new DocumentProcessingPublishException("document processing publication failed", exception);
		}
	}

	private static Duration requirePositive(Duration duration) {
		Objects.requireNonNull(duration, "confirm timeout is required");
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("confirm timeout must be positive");
		}
		return duration;
	}

}
