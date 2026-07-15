package com.rag.rag.application.port.out;

import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface DocumentProcessingRequestPort {

	UUID create(ProcessDocumentCommand command);

	Optional<DocumentProcessingClaim> claimForProcessing(UUID requestId, Duration leaseDuration);

	Optional<DocumentProcessingRequestState> findById(
		UUID workspaceId,
		UUID documentId,
		UUID requestId);

	DocumentProcessingRetryOutcome retryFailed(
		UUID workspaceId,
		UUID documentId,
		UUID requestId);

	void markCompleted(UUID requestId);

	void releaseForRetry(UUID requestId, String error);

	void markFailed(UUID requestId, String error);

}
