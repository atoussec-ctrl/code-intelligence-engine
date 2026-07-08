package com.rag.rag.application.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagValueObjectsTest {

	@Test
	void trimsCitationSourceTitle() {
		var chunkId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		var citation = new RagCitation(chunkId, documentId, " Source ");

		assertEquals(chunkId, citation.chunkId());
		assertEquals(documentId, citation.documentId());
		assertEquals("Source", citation.sourceTitle());
	}

	@Test
	void rejectsInvalidCitation() {
		var documentId = UUID.randomUUID();
		var chunkId = UUID.randomUUID();

		assertThrows(NullPointerException.class, () -> new RagCitation(null, documentId, "Source"));
		assertThrows(NullPointerException.class, () -> new RagCitation(chunkId, null, "Source"));
		var thrown = assertThrows(IllegalArgumentException.class, () -> new RagCitation(chunkId, documentId, " "));
		assertEquals("sourceTitle is required", thrown.getMessage());
	}

	@Test
	void trimsAnswerAndRequiresCitationsList() {
		var answer = new RagAnswer(" Answer ", List.of());

		assertEquals("Answer", answer.answer());
		assertThrows(UnsupportedOperationException.class, () -> answer.citations().add(citation()));
		assertThrows(NullPointerException.class, () -> new RagAnswer("Answer", null));
	}

	@Test
	void rejectsBlankAnswer() {
		var thrown = assertThrows(IllegalArgumentException.class, () -> new RagAnswer(" ", List.of()));

		assertEquals("answer is required", thrown.getMessage());
	}

	@Test
	void rejectsInvalidPromptSections() {
		assertThrows(IllegalArgumentException.class, () -> new RagPrompt(" ", "question", "context", "rules", "format"));
		assertThrows(IllegalArgumentException.class, () -> new RagPrompt("system", " ", "context", "rules", "format"));
		assertThrows(IllegalArgumentException.class, () -> new RagPrompt("system", "question", " ", "rules", "format"));
		assertThrows(IllegalArgumentException.class, () -> new RagPrompt("system", "question", "context", " ", "format"));
		assertThrows(IllegalArgumentException.class, () -> new RagPrompt("system", "question", "context", "rules", " "));
	}

	@Test
	void rejectsInvalidValidationResult() {
		var validWithErrors = assertThrows(
			IllegalArgumentException.class,
			() -> new RagOutputValidationResult(true, List.of("error")));
		assertEquals("valid result cannot contain errors", validWithErrors.getMessage());

		var invalidWithoutErrors = assertThrows(
			IllegalArgumentException.class,
			() -> RagOutputValidationResult.invalid(List.of()));
		assertEquals("invalid result requires errors", invalidWithoutErrors.getMessage());
	}

	@Test
	void rejectsInvalidRetrievedContext() {
		var workspaceId = UUID.randomUUID();
		var chunkId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		assertThrows(NullPointerException.class, () -> new RetrievedContext(null, chunkId, documentId, "Source", "Content", 0.7));
		assertThrows(NullPointerException.class, () -> new RetrievedContext(workspaceId, null, documentId, "Source", "Content", 0.7));
		assertThrows(NullPointerException.class, () -> new RetrievedContext(workspaceId, chunkId, null, "Source", "Content", 0.7));
		assertThrows(IllegalArgumentException.class, () -> new RetrievedContext(workspaceId, chunkId, documentId, " ", "Content", 0.7));
		assertThrows(IllegalArgumentException.class, () -> new RetrievedContext(workspaceId, chunkId, documentId, "Source", " ", 0.7));
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new RetrievedContext(workspaceId, chunkId, documentId, "Source", "Content", Double.NaN));
		assertEquals("score must be finite", thrown.getMessage());
	}

	private static RagCitation citation() {
		return new RagCitation(UUID.randomUUID(), UUID.randomUUID(), "Source");
	}

}