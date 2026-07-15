package com.rag.rag.adapter.in.messaging;

import com.rag.rag.adapter.messaging.DocumentProcessingMessage;
import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.util.Objects;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessingMessageRecoverer implements MessageRecoverer {

	private final DocumentProcessingRequestPort processingRequests;
	private final MessageConverter messageConverter;

	public DocumentProcessingMessageRecoverer(
		DocumentProcessingRequestPort processingRequests,
		MessageConverter messageConverter) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
		this.messageConverter = Objects.requireNonNull(messageConverter, "message converter is required");
	}

	@Override
	public void recover(Message message, Throwable cause) {
		Objects.requireNonNull(message, "message is required");
		Objects.requireNonNull(cause, "cause is required");
		try {
			var payload = messageConverter.fromMessage(message);
			if (payload instanceof DocumentProcessingMessage processingMessage) {
				processingRequests.markFailed(processingMessage.requestId(), failureReason(cause));
			}
		}
		finally {
			throw new AmqpRejectAndDontRequeueException(
				"document processing retries exhausted",
				cause);
		}
	}

	private static String failureReason(Throwable cause) {
		var failure = cause;
		while (failure.getCause() != null) {
			failure = failure.getCause();
		}
		return failure.getMessage() == null || failure.getMessage().isBlank()
			? failure.getClass().getSimpleName()
			: failure.getMessage();
	}

}
