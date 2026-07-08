package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessDocumentCommandTest {

	@Test
	void createsProcessDocumentCommand() {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		var command = new ProcessDocumentCommand(workspaceId, documentId, " content ", 256);

		assertEquals(workspaceId, command.workspaceId());
		assertEquals(documentId, command.documentId());
		assertEquals(" content ", command.content());
		assertEquals(256, command.maxTokens());
	}

	@Test
	void rejectsNullWorkspaceId() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new ProcessDocumentCommand(null, UUID.randomUUID(), "content", 256));

		assertEquals("workspace id is required", thrown.getMessage());
	}

	@Test
	void rejectsNullDocumentId() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new ProcessDocumentCommand(UUID.randomUUID(), null, "content", 256));

		assertEquals("document id is required", thrown.getMessage());
	}

	@Test
	void rejectsBlankContent() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), " ", 256));

		assertEquals("content is required", thrown.getMessage());
	}

	@Test
	void rejectsNonPositiveMaxTokens() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 0));

		assertEquals("max tokens must be positive", thrown.getMessage());
	}

}