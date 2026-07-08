package com.rag.rag.application.port.out;

import com.rag.rag.domain.embedding.EmbeddingVector;

import java.util.List;

public interface EmbeddingGeneratorPort {

	List<EmbeddingVector> generateBatch(List<String> texts);

}