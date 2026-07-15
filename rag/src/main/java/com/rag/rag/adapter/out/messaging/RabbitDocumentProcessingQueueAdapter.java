package com.rag.rag.adapter.out.messaging;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.port.out.DocumentProcessingQueuePort;
import com.rag.rag.application.usecase.DocumentProcessingUnavailableException;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.util.Objects;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitDocumentProcessingQueueAdapter implements DocumentProcessingQueuePort {

	private final RabbitTemplate rabbitTemplate;

	public RabbitDocumentProcessingQueueAdapter(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbit template is required");
	}

	@Override
	public void enqueue(ProcessDocumentCommand command) {
		try {
			rabbitTemplate.convertAndSend(
				RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE,
				RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY,
				DocumentProcessingMessage.from(command));
		}
		catch (AmqpException exception) {
			throw new DocumentProcessingUnavailableException(exception);
		}
	}

}
