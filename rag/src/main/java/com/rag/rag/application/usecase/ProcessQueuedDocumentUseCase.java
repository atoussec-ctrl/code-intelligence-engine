package com.rag.rag.application.usecase;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ProcessQueuedDocumentUseCase {

	private final DocumentProcessingRequestPort processingRequests;
	private final ProcessDocumentUseCase processDocument;
	private final Duration leaseDuration;

	public ProcessQueuedDocumentUseCase(
		DocumentProcessingRequestPort processingRequests,
		ProcessDocumentUseCase processDocument,
		Duration leaseDuration) {
		this.processingRequests = Objects.requireNonNull(
			processingRequests,
			"processing requests are required");
		this.processDocument = Objects.requireNonNull(processDocument, "process document is required");
		this.leaseDuration = requirePositive(leaseDuration);
	}

	public Optional<ProcessDocumentResult> execute(UUID requestId) {
		Objects.requireNonNull(requestId, "request id is required");
		var claim = processingRequests.claimForProcessing(requestId, leaseDuration)
			.orElseThrow(DocumentProcessingRequestNotFoundException::new);
		if (!claim.acquired()) {
			return Optional.empty();
		}
		try {
			var result = processDocument.execute(claim.command());
			processingRequests.markCompleted(requestId);
			return Optional.of(result);
		}
		catch (RuntimeException exception) {
			releaseForRetry(requestId, exception);
			throw exception;
		}
	}

	private void releaseForRetry(UUID requestId, RuntimeException exception) {
		try {
			processingRequests.releaseForRetry(requestId, failureReason(exception));
		}
		catch (RuntimeException releaseFailure) {
			exception.addSuppressed(releaseFailure);
		}
	}

	private static String failureReason(RuntimeException exception) {
		return exception.getMessage() == null || exception.getMessage().isBlank()
			? exception.getClass().getSimpleName()
			: exception.getMessage();
	}

	private static Duration requirePositive(Duration duration) {
		Objects.requireNonNull(duration, "processing lease duration is required");
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("processing lease duration must be positive");
		}
		return duration;
	}

}
