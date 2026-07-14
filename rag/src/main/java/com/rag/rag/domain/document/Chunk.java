package com.rag.rag.domain.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Chunk {

	private final UUID id;
	private final UUID workspaceId;
	private final UUID documentId;
	private final int chunkIndex;
	private final String content;
	private final int tokenCount;
	private final Map<String, String> metadata;

	private Chunk(
		UUID id,
		UUID workspaceId,
		UUID documentId,
		int chunkIndex,
		String content,
		int tokenCount,
		Map<String, String> metadata) {
		this.id = DomainValidation.requiredUuid(id, "chunk id");
		this.workspaceId = DomainValidation.requiredUuid(workspaceId, "workspace id");
		this.documentId = DomainValidation.requiredUuid(documentId, "document id");
		this.chunkIndex = requireNonNegative(chunkIndex, "chunk index");
		this.content = DomainValidation.requiredText(content, "chunk content");
		this.tokenCount = requirePositive(tokenCount, "token count");
		this.metadata = DomainValidation.metadata(metadata);
	}

	public static Chunk create(
		UUID workspaceId,
		UUID documentId,
		int chunkIndex,
		String content,
		int tokenCount,
		Map<String, String> metadata) {
		return new Chunk(
			UUID.randomUUID(),
			workspaceId,
			documentId,
			chunkIndex,
			content,
			tokenCount,
			metadata);
	}

	public UUID id() {
		return id;
	}

	public UUID workspaceId() {
		return workspaceId;
	}

	public UUID documentId() {
		return documentId;
	}

	public int chunkIndex() {
		return chunkIndex;
	}

	public String content() {
		return content;
	}

	public int tokenCount() {
		return tokenCount;
	}

	public Map<String, String> metadata() {
		return ImmutableMetadataMap.from(metadata, "Chunk metadata is immutable");
	}

	public Chunk withMetadata(Map<String, String> additionalMetadata) {
		var mergedMetadata = new LinkedHashMap<>(metadata);
		if (additionalMetadata != null) {
			mergedMetadata.putAll(additionalMetadata);
		}
		return new Chunk(
			id,
			workspaceId,
			documentId,
			chunkIndex,
			content,
			tokenCount,
			mergedMetadata);
	}

	private static int requireNonNegative(int value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " cannot be negative");
		}
		return value;
	}

	private static int requirePositive(int value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}

}
