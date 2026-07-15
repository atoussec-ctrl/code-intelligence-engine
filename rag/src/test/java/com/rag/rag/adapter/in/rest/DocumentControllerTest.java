package com.rag.rag.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rag.rag.application.usecase.RegisterDocumentCommand;
import com.rag.rag.application.usecase.RegisterDocumentResult;
import com.rag.rag.application.usecase.RegisterDocumentUseCase;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
class DocumentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegisterDocumentUseCase registerDocument;

	@Test
	void requiresAuthentication() throws Exception {
		var workspaceId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/documents", workspaceId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(registerDocument);
	}

	@Test
	void registersDocumentForAuthenticatedUser() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		when(registerDocument.execute(any(RegisterDocumentCommand.class)))
			.thenReturn(new RegisterDocumentResult(documentId, DocumentStatus.INGESTION_REQUESTED));

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/documents", workspaceId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isCreated())
			.andExpect(header().string(
				"Location",
				"/api/v1/workspaces/%s/documents/%s".formatted(workspaceId, documentId)))
			.andExpect(jsonPath("$.documentId").value(documentId.toString()))
			.andExpect(jsonPath("$.status").value("INGESTION_REQUESTED"));

		var command = ArgumentCaptor.forClass(RegisterDocumentCommand.class);
		verify(registerDocument).execute(command.capture());
		var captured = command.getValue();
		assertEquals(workspaceId, captured.workspaceId());
		assertEquals("Architecture Notes", captured.title());
		assertEquals(DocumentSourceType.URL, captured.source().type());
		assertEquals("https://example.com/docs", captured.source().uri());
		assertEquals("checksum-123", captured.checksum());
		assertEquals("architecture", captured.metadata().get("tag"));
	}

	@Test
	void returnsProblemDetailForInvalidRequest() throws Exception {
		var workspaceId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/documents", workspaceId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "Architecture Notes",
					  "sourceType": "URL",
					  "checksum": "checksum-123"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.detail").value("source uri is required"));

		verifyNoInteractions(registerDocument);
	}

	@Test
	void returnsProblemDetailForUnsupportedSourceType() throws Exception {
		var workspaceId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/documents", workspaceId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "Architecture Notes",
					  "sourceType": "S3",
					  "checksum": "checksum-123"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.detail")
				.value("request body is malformed or contains unsupported values"));

		verifyNoInteractions(registerDocument);
	}

	private String validRequest() {
		return """
			{
			  "title": "Architecture Notes",
			  "sourceType": "URL",
			  "sourceUri": "https://example.com/docs",
			  "checksum": "checksum-123",
			  "metadata": {
			    "tag": "architecture"
			  }
			}
			""";
	}
}
