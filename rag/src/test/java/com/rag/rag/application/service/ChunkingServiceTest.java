package com.rag.rag.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ChunkingServiceTest {

	private final ChunkingService chunkingService = new ChunkingService();

	@Test
	void splitsTextRespectingMaxTokens() {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var content = "one two three four five six seven eight nine ten";

		var chunks = chunkingService.split(
			workspaceId,
			documentId,
			content,
			4,
			Map.of("tag", "architecture"));

		assertEquals(3, chunks.size());
		assertEquals("one two three four", chunks.get(0).content());
		assertEquals("five six seven eight", chunks.get(1).content());
		assertEquals("nine ten", chunks.get(2).content());
		assertTrue(chunks.stream().allMatch(chunk -> chunk.tokenCount() <= 4));
	}

	@Test
	void createsChunksWithSequentialIndexesAndDocumentScope() {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		var chunks = chunkingService.split(
			workspaceId,
			documentId,
			"alpha beta gamma delta epsilon",
			2,
			Map.of());

		assertEquals(List.of(0, 1, 2), chunks.stream().map(chunk -> chunk.chunkIndex()).toList());
		assertTrue(chunks.stream().allMatch(chunk -> workspaceId.equals(chunk.workspaceId())));
		assertTrue(chunks.stream().allMatch(chunk -> documentId.equals(chunk.documentId())));
	}

	@Test
	void preservesMetadataInEveryChunk() {
		var chunks = chunkingService.split(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"alpha beta gamma delta",
			2,
			Map.of("section", "overview"));

		assertEquals(2, chunks.size());
		assertTrue(chunks.stream().allMatch(chunk -> "overview".equals(chunk.metadata().get("section"))));
	}

	@Test
	void rejectsInvalidMaxTokens() {
		var thrown = assertThrows(
			IllegalArgumentException.class, 
			() -> chunkingService
			.split(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"content",
			0,
			Map.of()
		));
		assertEquals("max tokens must be positive", thrown.getMessage());
	}

	@Test
	void rejectsBlankContent() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> chunkingService.split(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"   ",
			2,
			Map.of())
		);
		assertEquals("content is required", thrown.getMessage());
	}

}