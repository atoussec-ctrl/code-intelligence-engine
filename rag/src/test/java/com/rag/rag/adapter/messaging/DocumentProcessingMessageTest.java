package com.rag.rag.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentProcessingMessageTest {

	@Test
	void carriesOnlyProcessingRequestId() {
		var requestId = UUID.randomUUID();

		var message = new DocumentProcessingMessage(requestId);

		assertEquals(requestId, message.requestId());
	}

	@Test
	void rejectsNullRequestId() {
		var thrown = assertThrows(NullPointerException.class, () -> new DocumentProcessingMessage(null));

		assertEquals("request id is required", thrown.getMessage());
	}

}
