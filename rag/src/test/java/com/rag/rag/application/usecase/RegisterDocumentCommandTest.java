package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.domain.document.DocumentSource;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterDocumentCommandTest {

	@Test
	void createsRegisterDocumentCommand() {
		var workspaceId = UUID.randomUUID();
		var source = DocumentSource.url("https://example.com/docs");

		var command = new RegisterDocumentCommand(
			workspaceId,
			"Architecture Notes",
			source,
			"checksum-123",
			Map.of("tag", "architecture"));

		assertEquals(workspaceId, command.workspaceId());
		assertEquals("Architecture Notes", command.title());
		assertEquals(source, command.source());
		assertEquals("checksum-123", command.checksum());
		assertEquals("architecture", command.metadata().get("tag"));
	}

	@Test
	void rejectsNullWorkspaceId() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new RegisterDocumentCommand(
				null,
				"Architecture Notes",
				DocumentSource.text(),
				"checksum-123",
				Map.of()));

		assertEquals("workspace id is required", thrown.getMessage());
	}

	@Test
	void rejectsBlankTitle() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new RegisterDocumentCommand(
				UUID.randomUUID(),
				" ",
				DocumentSource.text(),
				"checksum-123",
				Map.of()));

		assertEquals("title is required", thrown.getMessage());
	}

	@Test
	void rejectsNullSource() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new RegisterDocumentCommand(
				UUID.randomUUID(),
				"Architecture Notes",
				null,
				"checksum-123",
				Map.of()));

		assertEquals("source is required", thrown.getMessage());
	}

	@Test
	void rejectsBlankChecksum() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new RegisterDocumentCommand(
				UUID.randomUUID(),
				"Architecture Notes",
				DocumentSource.text(),
				" ",
				Map.of()));

		assertEquals("checksum is required", thrown.getMessage());
	}

	@Test
	void copiesMetadata() {
		var metadata = new java.util.HashMap<String, String>();
		metadata.put("tag", "architecture");

		var command = new RegisterDocumentCommand(
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSource.text(),
			"checksum-123",
			metadata);

		metadata.put("tag", "changed");

		assertEquals("architecture", command.metadata().get("tag"));
	}
}
