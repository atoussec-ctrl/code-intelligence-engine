package com.rag.rag.application.usecase;

import com.rag.rag.domain.document.DocumentSource;
import java.util.Map;
import java.util.UUID;

public record RegisterDocumentCommand(
	UUID workspaceId,
	String title,
	DocumentSource source,
	String checksum,
	Map<String, String> metadata) {

	public RegisterDocumentCommand {
		if (workspaceId == null) {
			throw new IllegalArgumentException("workspace id is required");
		}
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title is required");
		}
		if (source == null) {
			throw new IllegalArgumentException("source is required");
		}
		if (checksum == null || checksum.isBlank()) {
			throw new IllegalArgumentException("checksum is required");
		}
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
