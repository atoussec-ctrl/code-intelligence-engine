package com.rag.rag.application.service;

import com.rag.rag.domain.document.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChunkingService {

	public List<Chunk> split(
		UUID workspaceId,
		UUID documentId,
		String content,
		int maxTokens,
		Map<String, String> metadata) {
		if (maxTokens <= 0) {
			throw new IllegalArgumentException("max tokens must be positive");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content is required");
		}

		var tokens = content.trim().split("\\s+");
		var chunks = new ArrayList<Chunk>();
		var chunkIndex = 0;

		for (var start = 0; start < tokens.length; start += maxTokens) {
			var end = Math.min(start + maxTokens, tokens.length);
			var chunkTokens = Arrays.copyOfRange(tokens, start, end);
			var chunkContent = String.join(" ", chunkTokens);

			chunks.add(Chunk.create(
				workspaceId,
				documentId,
				chunkIndex,
				chunkContent,
				chunkTokens.length,
				metadata));
			chunkIndex++;
		}

		return List.copyOf(chunks);
	}

}