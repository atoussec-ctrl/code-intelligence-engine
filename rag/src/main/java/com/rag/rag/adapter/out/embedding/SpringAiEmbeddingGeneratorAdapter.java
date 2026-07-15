package com.rag.rag.adapter.out.embedding;

import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.ai.embedding.EmbeddingModel;

public class SpringAiEmbeddingGeneratorAdapter implements EmbeddingGeneratorPort {

	private final EmbeddingModel embeddingModel;
	private final String modelName;

	public SpringAiEmbeddingGeneratorAdapter(EmbeddingModel embeddingModel, String modelName) {
		this.embeddingModel = Objects.requireNonNull(embeddingModel, "embedding model is required");
		if (modelName == null || modelName.isBlank()) {
			throw new IllegalArgumentException("embedding model name is required");
		}
		this.modelName = modelName.trim();
	}

	@Override
	public List<EmbeddingVector> generateBatch(List<String> texts) {
		Objects.requireNonNull(texts, "texts are required");
		if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
			throw new IllegalArgumentException("embedding text is required");
		}
		if (texts.isEmpty()) {
			return List.of();
		}

		var vectors = Objects.requireNonNull(
			embeddingModel.embed(List.copyOf(texts)),
			"embedding model response is required");
		return vectors.stream()
			.map(this::toEmbeddingVector)
			.toList();
	}

	private EmbeddingVector toEmbeddingVector(float[] vector) {
		if (vector == null || vector.length == 0) {
			throw new IllegalStateException("embedding model returned an empty vector");
		}
		var values = IntStream.range(0, vector.length)
			.mapToObj(index -> (double) vector[index])
			.toList();
		return EmbeddingVector.of(values, modelName);
	}
}
