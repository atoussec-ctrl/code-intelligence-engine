package com.rag.rag.domain.embedding;

import java.util.List;

public record EmbeddingVector(List<Double> values, String model) {

	public EmbeddingVector {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("embedding values are required");
		}
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("embedding model is required");
		}
		values = List.copyOf(values);
	}

	public static EmbeddingVector of(List<Double> values, String model) {
		return new EmbeddingVector(values, model);
	}

	public int dimensions() {
		return values.size();
	}

}