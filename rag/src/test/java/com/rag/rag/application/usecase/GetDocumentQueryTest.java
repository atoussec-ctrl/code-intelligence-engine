package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentQueryTest {

	@Test
	void createsQuery() {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		var query = new GetDocumentQuery(workspaceId, documentId);

		assertEquals(workspaceId, query.workspaceId());
		assertEquals(documentId, query.documentId());
	}

	@Test
	void requiresWorkspaceId() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new GetDocumentQuery(null, UUID.randomUUID()));

		assertEquals("workspace id is required", thrown.getMessage());
	}

	@Test
	void requiresDocumentId() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new GetDocumentQuery(UUID.randomUUID(), null));

		assertEquals("document id is required", thrown.getMessage());
	}
}
