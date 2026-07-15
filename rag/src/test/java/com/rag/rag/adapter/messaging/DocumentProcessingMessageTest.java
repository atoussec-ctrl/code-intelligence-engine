package com.rag.rag.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentProcessingMessageTest {

	@Test
	void mapsApplicationCommandToAndFromWireMessage() {
		var command = new ProcessDocumentCommand(UUID.randomUUID(), UUID.randomUUID(), "content", 256);

		var message = DocumentProcessingMessage.from(command);

		assertEquals(command, message.toCommand());
	}

	@Test
	void rejectsNullCommand() {
		var thrown = assertThrows(NullPointerException.class, () -> DocumentProcessingMessage.from(null));

		assertEquals("command is required", thrown.getMessage());
	}

}
