package com.rag.rag.domain.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DocumentSourceTest {

	@Test
	void createsSupportedDocumentSources() {
		var text = DocumentSource.text();
		var url = DocumentSource.url(" https://example.com/docs ");
		var github = DocumentSource.github(" https://github.com/example/repository ");
		var file = DocumentSource.file(" file:///documents/architecture.pdf ");

		assertEquals(DocumentSourceType.TEXT, text.type());
		assertNull(text.uri());
		assertEquals("https://example.com/docs", url.uri());
		assertEquals("https://github.com/example/repository", github.uri());
		assertEquals("file:///documents/architecture.pdf", file.uri());
	}

	@Test
	void requiresUriForExternalSources() {
		for (var type : new DocumentSourceType[] {
			DocumentSourceType.URL,
			DocumentSourceType.GITHUB,
			DocumentSourceType.FILE
		}) {
			var thrown = assertThrows(
				IllegalArgumentException.class,
				() -> new DocumentSource(type, " "));

			assertEquals("source uri is required", thrown.getMessage());
		}
	}

	@Test
	void rejectsUriForTextSource() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new DocumentSource(DocumentSourceType.TEXT, "file:///unexpected.txt"));

		assertEquals("source uri is not allowed for text sources", thrown.getMessage());
	}

	@Test
	void requiresSourceType() {
		var thrown = assertThrows(
			IllegalArgumentException.class,
			() -> new DocumentSource(null, null));

		assertEquals("source type is required", thrown.getMessage());
	}
}
