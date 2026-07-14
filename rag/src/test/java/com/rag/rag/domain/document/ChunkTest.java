package com.rag.rag.domain.document;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ChunkTest {

	@Test
	void createsChunkWithDocumentAndWorkspaceMetadata() {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();

		var chunk = Chunk.create(
			workspaceId,
			documentId,
			0,
			"Clean Architecture keeps policy away from infrastructure.",
			8,
			Map.of("section", "architecture"));

		assertNotNull(chunk.id());
		assertEquals(workspaceId, chunk.workspaceId());
		assertEquals(documentId, chunk.documentId());
		assertEquals(0, chunk.chunkIndex());
		assertEquals(8, chunk.tokenCount());
		assertEquals("architecture", chunk.metadata().get("section"));
	}

	@Test
	void rejectsBlankChunkContent() {
		var thrown = assertThrows(IllegalArgumentException.class, () -> Chunk.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			0,
			"   ",
			1,
			Map.of()));
		assertEquals("chunk content is required", thrown.getMessage());
	}

	@Test
	void rejectsNonPositiveTokenCount() {
		var thrown = assertThrows(IllegalArgumentException.class, () -> Chunk.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			0,
			"content",
			0,
			Map.of()));
		assertEquals("token count must be positive", thrown.getMessage());
	}

	@Test
	void protectsChunkMetadataFromExternalMutation() {
		var chunk = Chunk.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			0,
			"content",
			1,
			Map.of("section", "architecture"));

		var thrown = assertThrows(UnsupportedOperationException.class, () -> chunk.metadata().put("section", "changed"));
		assertEquals("Chunk metadata is immutable", thrown.getMessage());
	}

	@Test
	void annotatesChunkMetadataWithoutChangingIdentity() {
		var chunk = Chunk.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			0,
			"content",
			1,
			Map.of("section", "architecture"));

		var annotated = chunk.withMetadata(Map.of("security.injection_suspected", "true"));

		assertEquals(chunk.id(), annotated.id());
		assertEquals(chunk.workspaceId(), annotated.workspaceId());
		assertEquals(chunk.documentId(), annotated.documentId());
		assertEquals(chunk.chunkIndex(), annotated.chunkIndex());
		assertEquals(chunk.content(), annotated.content());
		assertEquals(chunk.tokenCount(), annotated.tokenCount());
		assertEquals("architecture", annotated.metadata().get("section"));
		assertEquals("true", annotated.metadata().get("security.injection_suspected"));
	}

}
