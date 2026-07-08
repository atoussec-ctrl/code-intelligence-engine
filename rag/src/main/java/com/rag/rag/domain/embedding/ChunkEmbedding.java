package com.rag.rag.domain.embedding;

import com.rag.rag.domain.document.Chunk;

public record ChunkEmbedding(Chunk chunk, EmbeddingVector embedding) {

	public ChunkEmbedding {
		if (chunk == null) {
			throw new IllegalArgumentException("chunk is required");
		}
		if (embedding == null) {
			throw new IllegalArgumentException("embedding is required");
		}
	}

	public static ChunkEmbedding of(Chunk chunk, EmbeddingVector embedding) {
		return new ChunkEmbedding(chunk, embedding);
	}

}