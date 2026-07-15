package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessQueuedDocumentUseCaseTest {

	@Test
	void processesPersistedCommandAndMarksRequestCompleted() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		var command = command();
		var result = new ProcessDocumentResult(command.documentId(), 3);
		when(requests.findCommandById(requestId)).thenReturn(Optional.of(command));
		when(processDocument.execute(command)).thenReturn(result);
		var useCase = new ProcessQueuedDocumentUseCase(requests, processDocument);

		var actual = useCase.execute(requestId);

		assertSame(result, actual);
		verify(requests).markCompleted(requestId);
	}

	@Test
	void rejectsUnknownRequestWithoutProcessing() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		when(requests.findCommandById(requestId)).thenReturn(Optional.empty());
		var useCase = new ProcessQueuedDocumentUseCase(requests, processDocument);

		var thrown = assertThrows(
			DocumentProcessingRequestNotFoundException.class,
			() -> useCase.execute(requestId));

		assertEquals("document processing request not found", thrown.getMessage());
		verifyNoInteractions(processDocument);
		verify(requests).findCommandById(requestId);
		verifyNoMoreInteractions(requests);
	}

	@Test
	void leavesRequestIncompleteWhenProcessingFails() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);
		var requestId = UUID.randomUUID();
		var command = command();
		when(requests.findCommandById(requestId)).thenReturn(Optional.of(command));
		doThrow(new IllegalStateException("embedding provider unavailable"))
			.when(processDocument).execute(command);
		var useCase = new ProcessQueuedDocumentUseCase(requests, processDocument);

		assertThrows(IllegalStateException.class, () -> useCase.execute(requestId));

		verify(requests).findCommandById(requestId);
		verifyNoMoreInteractions(requests);
	}

	@Test
	void validatesDependenciesAndRequestId() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var processDocument = mock(ProcessDocumentUseCase.class);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new ProcessQueuedDocumentUseCase(null, processDocument)).getMessage());
		assertEquals(
			"process document is required",
			assertThrows(
				NullPointerException.class,
				() -> new ProcessQueuedDocumentUseCase(requests, null)).getMessage());

		var useCase = new ProcessQueuedDocumentUseCase(requests, processDocument);
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(null)).getMessage());
	}

	private ProcessDocumentCommand command() {
		return new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);
	}

}
