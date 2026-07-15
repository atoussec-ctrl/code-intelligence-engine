package com.rag.rag.domain.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingVectorTest {

	@Test
	void createsImmutableEmbeddingVector() {
		var vector = EmbeddingVector.of(List.of(0.1, 0.2, 0.3), " test-embedding-model ");

		assertEquals("test-embedding-model", vector.model());
		assertEquals(3, vector.dimensions());
		assertEquals(List.of(0.1, 0.2, 0.3), vector.values());
		assertThrows(UnsupportedOperationException.class, () -> vector.values().add(0.4));
	}

	@Test
	void rejectsEmptyVector() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> EmbeddingVector.of(List.of(), "test-embedding-model"));

		assertEquals("embedding values are required", thrown.getMessage());
	}

	@Test
	void rejectsBlankModel() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> EmbeddingVector.of(List.of(0.1), "   "));

		assertEquals("embedding model is required", thrown.getMessage());
	}

	@Test
	void rejectsNonFiniteValues() {
		var nan = assertThrows(
			IllegalArgumentException.class,
			() -> EmbeddingVector.of(List.of(0.1, Double.NaN), "test-embedding-model"));
		var infinity = assertThrows(
			IllegalArgumentException.class,
			() -> EmbeddingVector.of(List.of(Double.POSITIVE_INFINITY), "test-embedding-model"));

		assertEquals("embedding values must be finite", nan.getMessage());
		assertEquals("embedding values must be finite", infinity.getMessage());
	}

}
