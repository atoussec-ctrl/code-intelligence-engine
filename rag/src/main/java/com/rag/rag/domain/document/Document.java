package com.rag.rag.domain.document;

import java.util.Map;
import java.util.UUID;

public final class Document {

	private final UUID id;
	private final UUID workspaceId;
	private final String title;
	private final DocumentSource source;
	private final String checksum;
	private final DocumentStatus status;
	private final Map<String, String> metadata;

	private Document(
		UUID id,
		UUID workspaceId,
		String title,
		DocumentSource source,
		String checksum,
		DocumentStatus status,
		Map<String, String> metadata) {
		this.id = DomainValidation.requiredUuid(id, "document id");
		this.workspaceId = DomainValidation.requiredUuid(workspaceId, "Workspace");
		this.title = DomainValidation.requiredText(title, "title");
		this.source = DomainValidation.required(source, "source");
		this.checksum = DomainValidation.requiredText(checksum, "checksum");
		this.status = DomainValidation.required(status, "status");
		this.metadata = DomainValidation.metadata(metadata);
	}

	public static Document create(
		UUID workspaceId,
		String title,
		DocumentSource source,
		String checksum,
		Map<String, String> metadata) {
		return new Document(
			UUID.randomUUID(),
			workspaceId,
			title,
			source,
			checksum,
			DocumentStatus.INGESTION_REQUESTED,
			metadata);
	}

	public static Document restore(
		UUID id,
		UUID workspaceId,
		String title,
		DocumentSource source,
		String checksum,
		DocumentStatus status,
		Map<String, String> metadata) {
		return new Document(
			id,
			workspaceId,
			title,
			source,
			checksum,
			status,
			metadata);
	}

	public UUID id() {
		return id;
	}

	public UUID workspaceId() {
		return workspaceId;
	}

	public String title() {
		return title;
	}

	public DocumentSource source() {
		return source;
	}

	public String checksum() {
		return checksum;
	}

	public DocumentStatus status() {
		return status;
	}

	public Map<String, String> metadata() {
		return ImmutableMetadataMap.from(metadata, "Document metadata is immutable");
	}

}
