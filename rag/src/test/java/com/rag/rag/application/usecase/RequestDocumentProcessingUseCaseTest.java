package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestDocumentProcessingUseCaseTest {

	@Test
	void enqueuesExistingDocumentForProcessing() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingRequests = mock(DocumentProcessingRequestPort.class);
		var command = command();
		var requestId = UUID.randomUUID();
		when(documents.findById(command.workspaceId(), command.documentId()))
			.thenReturn(Optional.of(mock(Document.class)));
		when(processingRequests.create(command)).thenReturn(requestId);
		var useCase = new RequestDocumentProcessingUseCase(documents, processingRequests);

		var result = useCase.execute(command);

		assertEquals(requestId, result);
		verify(processingRequests).create(command);
	}

	@Test
	void rejectsDocumentOutsideWorkspaceWithoutPublishing() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingRequests = mock(DocumentProcessingRequestPort.class);
		var command = command();
		when(documents.findById(command.workspaceId(), command.documentId())).thenReturn(Optional.empty());
		var useCase = new RequestDocumentProcessingUseCase(documents, processingRequests);

		var thrown = assertThrows(DocumentNotFoundException.class, () -> useCase.execute(command));

		assertEquals("document not found", thrown.getMessage());
		verifyNoInteractions(processingRequests);
	}

	@Test
	void validatesDependenciesAndCommand() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingRequests = mock(DocumentProcessingRequestPort.class);

		assertEquals(
			"documents is required",
			assertThrows(
				NullPointerException.class,
				() -> new RequestDocumentProcessingUseCase(null, processingRequests)).getMessage());
		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new RequestDocumentProcessingUseCase(documents, null)).getMessage());

		var useCase = new RequestDocumentProcessingUseCase(documents, processingRequests);
		assertEquals(
			"command is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(null)).getMessage());
	}

	private ProcessDocumentCommand command() {
		return new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);
	}

}
