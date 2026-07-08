package com.rag.rag.domain.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.domain.document.Chunk;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChunkEmbeddingTest {

	@Test
	void createsChunkEmbedding() {
		var chunk = chunk();
		var embedding = EmbeddingVector.of(List.of(0.1, 0.2), "test-model");

		var chunkEmbedding = ChunkEmbedding.of(chunk, embedding);

		assertEquals(chunk, chunkEmbedding.chunk());
		assertEquals(embedding, chunkEmbedding.embedding());
	}

	@Test
	void rejectsNullChunk() {
		var embedding = EmbeddingVector.of(List.of(0.1), "test-model");

		var thrown = assertThrows(IllegalArgumentException.class, () -> ChunkEmbedding.of(null, embedding));

		assertEquals("chunk is required", thrown.getMessage());
	}

	@Test
	void rejectsNullEmbedding() {
		var thrown = assertThrows(IllegalArgumentException.class, () -> ChunkEmbedding.of(chunk(), null));

		assertEquals("embedding is required", thrown.getMessage());
	}

	private static Chunk chunk() {
		return Chunk.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			0,
			"chunk content",
			2,
			Map.of("source", "test"));
	}

}