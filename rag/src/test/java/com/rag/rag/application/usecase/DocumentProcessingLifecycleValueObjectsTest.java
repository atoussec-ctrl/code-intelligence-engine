package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rag.rag.application.port.out.DocumentProcessingClaim;
import com.rag.rag.application.port.out.DocumentProcessingRequestState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentProcessingLifecycleValueObjectsTest {

	@Test
	void identifiesTerminalStatuses() {
		assertTrue(DocumentProcessingStatus.COMPLETED.isTerminal());
		assertTrue(DocumentProcessingStatus.FAILED.isTerminal());
		assertFalse(DocumentProcessingStatus.PROCESSING.isTerminal());
	}

	@Test
	void modelsAcquiredAndSkippedClaims() {
		var command = new ProcessDocumentCommand(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"content",
			256);

		var acquired = DocumentProcessingClaim.acquired(command);
		var skipped = DocumentProcessingClaim.notAcquired(DocumentProcessingStatus.COMPLETED);

		assertTrue(acquired.acquired());
		assertEquals(command, acquired.command());
		assertFalse(skipped.acquired());
		assertEquals(DocumentProcessingStatus.COMPLETED, skipped.status());
		assertEquals(
			"an acquired claim must be processing",
			assertThrows(
				IllegalArgumentException.class,
				() -> new DocumentProcessingClaim(DocumentProcessingStatus.PUBLISHED, command)).getMessage());
		assertEquals(
			"status is required",
			assertThrows(
				NullPointerException.class,
				() -> DocumentProcessingClaim.notAcquired(null)).getMessage());
	}

	@Test
	void validatesProcessingRequestState() {
		var now = Instant.parse("2026-07-15T04:00:00Z");

		assertEquals(
			"request id is required",
			assertThrows(
				NullPointerException.class,
				() -> state(null, UUID.randomUUID(), UUID.randomUUID(), now, 0, 0)).getMessage());
		assertEquals(
			"attempt counts must not be negative",
			assertThrows(
				IllegalArgumentException.class,
				() -> state(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now, -1, 0))
				.getMessage());
	}

	@Test
	void validatesStatusQueryScope() {
		assertEquals(
			"workspace id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new GetDocumentProcessingRequestQuery(null, UUID.randomUUID(), UUID.randomUUID()))
				.getMessage());
		assertEquals(
			"document id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new GetDocumentProcessingRequestQuery(UUID.randomUUID(), null, UUID.randomUUID()))
				.getMessage());
		assertEquals(
			"request id is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new GetDocumentProcessingRequestQuery(UUID.randomUUID(), UUID.randomUUID(), null))
				.getMessage());
	}

	private DocumentProcessingRequestState state(
		UUID requestId,
		UUID workspaceId,
		UUID documentId,
		Instant now,
		int dispatchAttempts,
		int processingAttempts) {
		return new DocumentProcessingRequestState(
			requestId,
			workspaceId,
			documentId,
			DocumentProcessingStatus.PENDING,
			dispatchAttempts,
			processingAttempts,
			null,
			now,
			now,
			null,
			null,
			null);
	}

}
