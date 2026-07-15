package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.DocumentProcessingRequestPort;
import com.rag.rag.application.port.out.DocumentProcessingRequestState;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentProcessingRequestUseCaseTest {

	@Test
	void returnsScopedProcessingRequestState() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var query = query();
		var createdAt = Instant.parse("2026-07-15T04:00:00Z");
		var state = new DocumentProcessingRequestState(
			query.requestId(),
			query.workspaceId(),
			query.documentId(),
			DocumentProcessingStatus.FAILED,
			2,
			3,
			"embedding provider unavailable",
			createdAt,
			createdAt.plusSeconds(30),
			createdAt.plusSeconds(2),
			null,
			createdAt.plusSeconds(30));
		when(requests.findById(query.workspaceId(), query.documentId(), query.requestId()))
			.thenReturn(Optional.of(state));
		var useCase = new GetDocumentProcessingRequestUseCase(requests);

		var result = useCase.execute(query);

		assertEquals(state.requestId(), result.requestId());
		assertEquals(state.status(), result.status());
		assertEquals(state.processingAttempts(), result.processingAttempts());
		assertEquals(state.lastError(), result.lastError());
		assertEquals(state.failedAt(), result.failedAt());
		verify(requests).findById(query.workspaceId(), query.documentId(), query.requestId());
	}

	@Test
	void rejectsUnknownProcessingRequest() {
		var requests = mock(DocumentProcessingRequestPort.class);
		var query = query();
		when(requests.findById(query.workspaceId(), query.documentId(), query.requestId()))
			.thenReturn(Optional.empty());
		var useCase = new GetDocumentProcessingRequestUseCase(requests);

		assertThrows(DocumentProcessingRequestNotFoundException.class, () -> useCase.execute(query));
	}

	@Test
	void validatesDependencyAndQuery() {
		var requests = mock(DocumentProcessingRequestPort.class);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new GetDocumentProcessingRequestUseCase(null)).getMessage());
		assertEquals(
			"query is required",
			assertThrows(
				NullPointerException.class,
				() -> new GetDocumentProcessingRequestUseCase(requests).execute(null)).getMessage());
	}

	private GetDocumentProcessingRequestQuery query() {
		return new GetDocumentProcessingRequestQuery(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID());
	}

}
