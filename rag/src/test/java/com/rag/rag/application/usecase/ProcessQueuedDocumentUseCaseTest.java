package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingClaim;
import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessQueuedDocumentUseCaseTest {

	private static final Duration LEASE_DURATION = Duration.ofMinutes(30);

	@Test
	void processesAcquiredCommandAndMarksRequestCompleted() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		var command = command();
		var result = new ProcessDocumentResult(command.documentId(), 3);
		when(requests.claimForProcessing(requestId, LEASE_DURATION))
			.thenReturn(Optional.of(DocumentProcessingClaim.acquired(command)));
		when(processDocument.execute(command)).thenReturn(result);
		var useCase = useCase(requests, processDocument);

		var actual = useCase.execute(requestId);

		assertSame(result, actual.orElseThrow());
		verify(requests).markCompleted(requestId);
	}

	@Test
	void skipsDuplicateRequestAlreadyBeingProcessedOrCompleted() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		when(requests.claimForProcessing(requestId, LEASE_DURATION)).thenReturn(Optional.of(
			DocumentProcessingClaim.notAcquired(DocumentProcessingStatus.COMPLETED)));
		var useCase = useCase(requests, processDocument);

		assertTrue(useCase.execute(requestId).isEmpty());

		verifyNoInteractions(processDocument);
		verify(requests).claimForProcessing(requestId, LEASE_DURATION);
		verifyNoMoreInteractions(requests);
	}

	@Test
	void rejectsUnknownRequestWithoutProcessing() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		when(requests.claimForProcessing(requestId, LEASE_DURATION)).thenReturn(Optional.empty());
		var useCase = useCase(requests, processDocument);

		var thrown = assertThrows(
			DocumentProcessingRequestNotFoundException.class,
			() -> useCase.execute(requestId));

		assertEquals("document processing request not found", thrown.getMessage());
		verifyNoInteractions(processDocument);
		verify(requests).claimForProcessing(requestId, LEASE_DURATION);
		verifyNoMoreInteractions(requests);
	}

	@Test
	void releasesRequestForRetryWhenProcessingFails() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		var command = command();
		when(requests.claimForProcessing(requestId, LEASE_DURATION))
			.thenReturn(Optional.of(DocumentProcessingClaim.acquired(command)));
		doThrow(new IllegalStateException("embedding provider unavailable"))
			.when(processDocument).execute(command);
		var useCase = useCase(requests, processDocument);

		assertThrows(IllegalStateException.class, () -> useCase.execute(requestId));

		verify(requests).releaseForRetry(requestId, "embedding provider unavailable");
	}

	@Test
	void preservesProcessingFailureWhenReleasingTheClaimAlsoFails() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		var command = command();
		var processingFailure = new IllegalStateException();
		var releaseFailure = new IllegalStateException("database unavailable");
		when(requests.claimForProcessing(requestId, LEASE_DURATION))
			.thenReturn(Optional.of(DocumentProcessingClaim.acquired(command)));
		doThrow(processingFailure).when(processDocument).execute(command);
		doThrow(releaseFailure).when(requests).releaseForRetry(requestId, "IllegalStateException");
		var useCase = useCase(requests, processDocument);

		var thrown = assertThrows(IllegalStateException.class, () -> useCase.execute(requestId));

		assertSame(processingFailure, thrown);
		assertSame(releaseFailure, thrown.getSuppressed()[0]);
	}

	@Test
	void validatesDependenciesLeaseAndRequestId() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new ProcessQueuedDocumentUseCase(null, processDocument, LEASE_DURATION)).getMessage());
		assertEquals(
			"process document is required",
			assertThrows(
				NullPointerException.class,
				() -> new ProcessQueuedDocumentUseCase(requests, null, LEASE_DURATION)).getMessage());
		assertEquals(
			"processing lease duration is required",
			assertThrows(
				NullPointerException.class,
				() -> new ProcessQueuedDocumentUseCase(requests, processDocument, null)).getMessage());
		assertEquals(
			"processing lease duration must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> new ProcessQueuedDocumentUseCase(requests, processDocument, Duration.ZERO)).getMessage());

		var useCase = useCase(requests, processDocument);
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(null)).getMessage());
	}

	private ProcessQueuedDocumentUseCase useCase(
		DocumentProcessingRequestPort requests,
		ProcessDocumentUseCase processDocument) {
		return new ProcessQueuedDocumentUseCase(requests, processDocument, LEASE_DURATION);
	}

	private ProcessDocumentCommand command() {
		return new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);
	}

}
