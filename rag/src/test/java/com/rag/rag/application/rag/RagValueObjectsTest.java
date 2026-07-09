package com.rag.rag.application.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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

		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RagCitation(null, documentId, "Source"),
				"chunkId is required");
		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RagCitation(chunkId, null, "Source"),
				"documentId is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagCitation(chunkId, documentId, " "),
				"sourceTitle is required");
	}

	@Test
	void trimsAnswerAndRequiresCitationsList() {
		var answer = new RagAnswer(" Answer ", List.of());

		assertEquals("Answer", answer.answer());
		assertThrowsException(UnsupportedOperationException.class, () -> answer.citations().add(citation()));
		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RagAnswer("Answer", null),
				"citations are required");
	}

	@Test
	void rejectsBlankAnswer() {
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagAnswer(" ", List.of()),
				"answer is required");
	}

	@Test
	void rejectsInvalidPromptSections() {
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagPrompt(" ", "question", "context", "rules", "format"),
				"systemInstructions is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagPrompt("system", " ", "context", "rules", "format"),
				"userQuestion is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagPrompt("system", "question", " ", "rules", "format"),
				"retrievedContext is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagPrompt("system", "question", "context", " ", "format"),
				"citationRules is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RagPrompt("system", "question", "context", "rules", " "),
				"responseFormat is required");
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

		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RetrievedContext(null, chunkId, documentId, "Source", "Content", 0.7),
				"workspaceId is required");
		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RetrievedContext(workspaceId, null, documentId, "Source", "Content", 0.7),
				"chunkId is required");
		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new RetrievedContext(workspaceId, chunkId, null, "Source", "Content", 0.7),
				"documentId is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RetrievedContext(workspaceId, chunkId, documentId, " ", "Content", 0.7),
				"sourceTitle is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RetrievedContext(workspaceId, chunkId, documentId, "Source", " ", 0.7),
				"content is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new RetrievedContext(workspaceId, chunkId, documentId, "Source", "Content", Double.NaN),
				"score must be finite");
	}

	@Test
	void rejectsInvalidVectorSearchQuery() {
		var workspaceId = UUID.randomUUID();
		var embedding = EmbeddingVector.of(List.of(0.1), "test-model");

		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new VectorSearchQuery(null, embedding, 3),
				"workspaceId is required");
		assertThrowsWithMessage(
				NullPointerException.class,
				() -> new VectorSearchQuery(workspaceId, null, 3),
				"embedding is required");
		assertThrowsWithMessage(
				IllegalArgumentException.class,
				() -> new VectorSearchQuery(workspaceId, embedding, 0),
				"topK must be positive");
	}

	private static RagCitation citation() {
		return new RagCitation(UUID.randomUUID(), UUID.randomUUID(), "Source");
	}

	private static <T extends Throwable> void assertThrowsException(Class<T> expectedType, Executable executable) {
		assertNotNull(assertThrows(expectedType, executable));
	}

	private static <T extends Throwable> void assertThrowsWithMessage(
			Class<T> expectedType,
			Executable executable,
			String expectedMessage) {
		var thrown = assertThrows(expectedType, executable);
		assertEquals(expectedMessage, thrown.getMessage());
	}

}
