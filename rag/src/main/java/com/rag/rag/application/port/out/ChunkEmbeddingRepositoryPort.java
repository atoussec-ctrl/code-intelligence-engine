package com.rag.rag.application.port.out;

import com.rag.rag.domain.embedding.ChunkEmbedding;

import java.util.List;
import java.util.UUID;

public interface ChunkEmbeddingRepositoryPort {

	void saveAll(UUID workspaceId, UUID documentId, List<ChunkEmbedding> chunks);

}