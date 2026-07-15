package com.rag.rag.adapter.out.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiEmbeddingGeneratorAdapterTest {

	@Test
	void generatesOrderedDomainEmbeddingVectors() {
		var model = mock(EmbeddingModel.class);
		var texts = List.of("first chunk", "second chunk");
		when(model.embed(texts)).thenReturn(List.of(
			new float[] { 0.1f, 0.2f },
			new float[] { 0.3f, 0.4f }));
		var adapter = new SpringAiEmbeddingGeneratorAdapter(model, " mxbai-embed-large ");

		var result = adapter.generateBatch(texts);

		assertEquals(2, result.size());
		assertEquals(List.of((double) 0.1f, (double) 0.2f), result.get(0).values());
		assertEquals(List.of((double) 0.3f, (double) 0.4f), result.get(1).values());
		assertEquals("mxbai-embed-large", result.get(0).model());
		assertEquals("mxbai-embed-large", result.get(1).model());
	}

	@Test
	void avoidsProviderCallForEmptyBatch() {
		var model = mock(EmbeddingModel.class);
		var adapter = new SpringAiEmbeddingGeneratorAdapter(model, "mxbai-embed-large");

		assertEquals(List.of(), adapter.generateBatch(List.of()));
		verifyNoInteractions(model);
	}

	@Test
	void validatesConfigurationAndInput() {
		var model = mock(EmbeddingModel.class);

		assertEquals(
			"embedding model is required",
			assertThrows(
				NullPointerException.class,
				() -> new SpringAiEmbeddingGeneratorAdapter(null, "model"))
				.getMessage());
		assertEquals(
			"embedding model name is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new SpringAiEmbeddingGeneratorAdapter(model, " "))
				.getMessage());

		var adapter = new SpringAiEmbeddingGeneratorAdapter(model, "model");
		assertEquals(
			"texts are required",
			assertThrows(NullPointerException.class, () -> adapter.generateBatch(null)).getMessage());
		assertEquals(
			"embedding text is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> adapter.generateBatch(List.of("valid", " ")))
				.getMessage());
	}

	@Test
	void rejectsInvalidProviderResponses() {
		var model = mock(EmbeddingModel.class);
		var adapter = new SpringAiEmbeddingGeneratorAdapter(model, "model");
		var texts = List.of("chunk");

		when(model.embed(texts)).thenReturn(null);
		assertEquals(
			"embedding model response is required",
			assertThrows(NullPointerException.class, () -> adapter.generateBatch(texts)).getMessage());

		when(model.embed(texts)).thenReturn(java.util.Collections.singletonList(null));
		assertEquals(
			"embedding model returned an empty vector",
			assertThrows(IllegalStateException.class, () -> adapter.generateBatch(texts)).getMessage());

		when(model.embed(texts)).thenReturn(List.of(new float[0]));
		assertEquals(
			"embedding model returned an empty vector",
			assertThrows(IllegalStateException.class, () -> adapter.generateBatch(texts)).getMessage());
	}
}
