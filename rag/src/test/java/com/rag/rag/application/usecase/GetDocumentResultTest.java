package com.rag.rag.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentResultTest {

	@Test
	void copiesAndProtectsMetadata() {
		var metadata = new HashMap<String, String>();
		metadata.put("tag", "architecture");
		var result = result(metadata);

		metadata.put("tag", "changed");

		assertEquals("architecture", result.metadata().get("tag"));
		assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("tag", "changed"));
	}

	@Test
	void normalizesNullMetadata() {
		assertTrue(result(null).metadata().isEmpty());
	}

	private GetDocumentResult result(Map<String, String> metadata) {
		return new GetDocumentResult(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"Architecture Notes",
			DocumentSourceType.TEXT,
			null,
			"checksum-123",
			DocumentStatus.READY,
			metadata);
	}
}
