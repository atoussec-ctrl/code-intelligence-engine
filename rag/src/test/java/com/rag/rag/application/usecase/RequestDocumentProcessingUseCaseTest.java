package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingQueuePort;
import com.rag.rag.application.port.out.DocumentRepositoryPort;
import com.rag.rag.domain.document.Document;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestDocumentProcessingUseCaseTest {

	@Test
	void enqueuesExistingDocumentForProcessing() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingQueue = mock(DocumentProcessingQueuePort.class);
		var command = command();
		when(documents.findById(command.workspaceId(), command.documentId()))
			.thenReturn(Optional.of(mock(Document.class)));
		var useCase = new RequestDocumentProcessingUseCase(documents, processingQueue);

		useCase.execute(command);

		verify(processingQueue).enqueue(command);
	}

	@Test
	void rejectsDocumentOutsideWorkspaceWithoutPublishing() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingQueue = mock(DocumentProcessingQueuePort.class);
		var command = command();
		when(documents.findById(command.workspaceId(), command.documentId())).thenReturn(Optional.empty());
		var useCase = new RequestDocumentProcessingUseCase(documents, processingQueue);

		var thrown = assertThrows(DocumentNotFoundException.class, () -> useCase.execute(command));

		assertEquals("document not found", thrown.getMessage());
		verifyNoInteractions(processingQueue);
	}

	@Test
	void validatesDependenciesAndCommand() {
		var documents = mock(DocumentRepositoryPort.class);
		var processingQueue = mock(DocumentProcessingQueuePort.class);

		assertEquals(
			"documents is required",
			assertThrows(
				NullPointerException.class,
				() -> new RequestDocumentProcessingUseCase(null, processingQueue)).getMessage());
		assertEquals(
			"processing queue is required",
			assertThrows(
				NullPointerException.class,
				() -> new RequestDocumentProcessingUseCase(documents, null)).getMessage());

		var useCase = new RequestDocumentProcessingUseCase(documents, processingQueue);
		assertEquals(
			"command is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(null)).getMessage());
	}

	private ProcessDocumentCommand command() {
		return new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);
	}

}
