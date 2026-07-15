package com.rag.rag.adapter.in.messaging;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.usecase.ProcessDocumentUseCase;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.util.Objects;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitDocumentProcessingListener {

	private final ProcessDocumentUseCase processDocument;

	public RabbitDocumentProcessingListener(ProcessDocumentUseCase processDocument) {
		this.processDocument = Objects.requireNonNull(processDocument, "process document is required");
	}

	@RabbitListener(queues = RabbitDocumentProcessingConfig.PROCESSING_QUEUE)
	public void handle(DocumentProcessingMessage message) {
		processDocument.execute(message.toCommand());
	}

}
