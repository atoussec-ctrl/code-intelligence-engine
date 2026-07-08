package com.rag.rag.domain.document;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {

	@Test
	void createsDocumentWithInitialIngestionRequestedStatus() {
		var workspaceId = UUID.randomUUID();
		var metadata = Map.of("tag", "architecture");

		var document = Document.create(
			workspaceId,
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			metadata);

		assertNotNull(document.id());
		assertEquals(workspaceId, document.workspaceId());
		assertEquals("Architecture Notes", document.title());
		assertEquals(DocumentStatus.INGESTION_REQUESTED, document.status());
		assertEquals(DocumentSourceType.TEXT, document.source().type());
		assertEquals("architecture", document.metadata().get("tag"));
	}

	@Test
	void rejectsDocumentWithoutWorkspace() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> Document.create(
				null,
				"Title",
				DocumentSource.text(),
				"checksum",
				Map.of()
			));

		assertEquals("Workspace is required", thrown.getMessage());
	}

	@Test
	void protectsDocumentMetadataFromExternalMutation() {
		var document = Document.create(
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of("tag", "architecture"));

		var thrown = assertThrows(
			UnsupportedOperationException.class, 
			() -> document.metadata().put("tag", "changed")
		);
		assertEquals("Document metadata is immutable", thrown.getMessage());
	}

	@Test
	void copiesDocumentMetadataOnCreate() {
		var metadata = new HashMap<String, String>();
		metadata.put("tag", "architecture");

		var document = Document.create(
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			metadata);

		metadata.put("tag", "changed");

		assertEquals("architecture", document.metadata().get("tag"));
	}

	@Test
	void rejectsAllDocumentMetadataMutationOperations() {
		var document = Document.create(
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			Map.of("tag", "architecture"));

		assertEquals(
			"Document metadata is immutable",
			assertThrows(UnsupportedOperationException.class, () -> document.metadata().putAll(Map.of("tag", "changed"))).getMessage());
		assertEquals(
			"Document metadata is immutable",
			assertThrows(UnsupportedOperationException.class, () -> document.metadata().remove("tag")).getMessage());
		assertEquals(
			"Document metadata is immutable",
			assertThrows(UnsupportedOperationException.class, () -> document.metadata().clear()).getMessage());
	}

}
