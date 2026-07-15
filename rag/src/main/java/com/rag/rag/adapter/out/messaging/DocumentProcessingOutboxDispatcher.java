package com.rag.rag.adapter.out.messaging;

import com.rag.rag.adapter.out.persistence.ClaimedDocumentProcessingRequest;
import com.rag.rag.adapter.out.persistence.PostgresDocumentProcessingRequestAdapter;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
	name = "rag.document-processing.dispatcher.enabled",
	havingValue = "true",
	matchIfMissing = true)
public class DocumentProcessingOutboxDispatcher {

	private static final int MAX_BACKOFF_EXPONENT = 6;

	private final PostgresDocumentProcessingRequestAdapter processingRequests;
	private final RabbitDocumentProcessingPublisher publisher;
	private final int batchSize;
	private final Duration leaseDuration;

	public DocumentProcessingOutboxDispatcher(
		PostgresDocumentProcessingRequestAdapter processingRequests,
		RabbitDocumentProcessingPublisher publisher,
		@Value("${rag.document-processing.dispatcher.batch-size:20}") int batchSize,
		@Value("${rag.document-processing.dispatcher.lease-duration:30s}") Duration leaseDuration) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
		this.publisher = Objects.requireNonNull(publisher, "publisher is required");
		if (batchSize <= 0) {
			throw new IllegalArgumentException("batch size must be positive");
		}
		this.batchSize = batchSize;
		this.leaseDuration = requirePositive(leaseDuration);
	}

	@Scheduled(
		fixedDelayString = "${rag.document-processing.dispatcher.poll-interval:1s}",
		initialDelayString = "${rag.document-processing.dispatcher.initial-delay:1s}")
	public void dispatch() {
		processingRequests.claimPending(batchSize, leaseDuration).forEach(this::publish);
	}

	private void publish(ClaimedDocumentProcessingRequest request) {
		try {
			publisher.publish(request.requestId());
			processingRequests.markPublished(request.requestId());
		}
		catch (RuntimeException exception) {
			processingRequests.reschedule(
				request.requestId(),
				failureReason(exception),
				retryDelay(request.attempt()));
		}
	}

	private static Duration retryDelay(int attempt) {
		var exponent = Math.min(Math.max(attempt - 1, 0), MAX_BACKOFF_EXPONENT);
		return Duration.ofSeconds(1L << exponent);
	}

	private static String failureReason(RuntimeException exception) {
		return exception.getMessage() == null || exception.getMessage().isBlank()
			? exception.getClass().getSimpleName()
			: exception.getMessage();
	}

	private static Duration requirePositive(Duration duration) {
		Objects.requireNonNull(duration, "lease duration is required");
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("lease duration must be positive");
		}
		return duration;
	}

}
