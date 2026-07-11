package com.rag.rag.application.port.out;

import java.util.List;

import com.rag.rag.domain.embedding.EmbeddingVector;

public interface EmbeddingGeneratorPort {
	List<EmbeddingVector> generateBatch(List<String> texts);

}
