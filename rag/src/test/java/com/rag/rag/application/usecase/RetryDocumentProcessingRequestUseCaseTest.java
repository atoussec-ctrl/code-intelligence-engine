package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentProcessingRetryOutcome;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetryDocumentProcessingRequestUseCaseTest {

	@Test
	void retriesFailedProcessingRequest() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var command = command();
		when(requests.retryFailed(command.workspaceId(), command.documentId(), command.requestId()))
			.thenReturn(DocumentProcessingRetryOutcome.RETRIED);
		var useCase = new RetryDocumentProcessingRequestUseCase(requests);

		useCase.execute(command);

		verify(requests).retryFailed(
			command.workspaceId(),
			command.documentId(),
			command.requestId());
	}

	@Test
	void rejectsUnknownProcessingRequest() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var command = command();
		when(requests.retryFailed(command.workspaceId(), command.documentId(), command.requestId()))
			.thenReturn(DocumentProcessingRetryOutcome.NOT_FOUND);

		assertThrows(
			DocumentProcessingRequestNotFoundException.class,
			() -> new RetryDocumentProcessingRequestUseCase(requests).execute(command));
	}

	@Test
	void rejectsProcessingRequestThatHasNotFailed() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var command = command();
		when(requests.retryFailed(command.workspaceId(), command.documentId(), command.requestId()))
			.thenReturn(DocumentProcessingRetryOutcome.NOT_FAILED);

		var exception = assertThrows(
			DocumentProcessingRequestNotRetryableException.class,
			() -> new RetryDocumentProcessingRequestUseCase(requests).execute(command));

		assertEquals("only failed document processing requests can be retried", exception.getMessage());
	}

	@Test
	void validatesDependencyCommandAndScope() {
		var requests = mock(DocumentProcessingRequestPort.class);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new RetryDocumentProcessingRequestUseCase(null)).getMessage());
		assertEquals(
			"command is required",
			assertThrows(
				NullPointerException.class,
				() -> new RetryDocumentProcessingRequestUseCase(requests).execute(null)).getMessage());
		assertEquals(
			"retry outcome is required",
			assertThrows(
				NullPointerException.class,
				() -> new RetryDocumentProcessingRequestUseCase(requests).execute(command())).getMessage());
		assertEquals(
			"workspace id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new RetryDocumentProcessingRequestCommand(
					null,
					UUID.randomUUID(),
					UUID.randomUUID())).getMessage());
		assertEquals(
			"document id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new RetryDocumentProcessingRequestCommand(
					UUID.randomUUID(),
					null,
					UUID.randomUUID())).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new RetryDocumentProcessingRequestCommand(
					UUID.randomUUID(),
					UUID.randomUUID(),
					null)).getMessage());
	}

	private RetryDocumentProcessingRequestCommand command() {
		return new RetryDocumentProcessingRequestCommand(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID());
	}

}
