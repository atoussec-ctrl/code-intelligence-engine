package com.rag.rag.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rag.rag.application.usecase.DocumentNotFoundException;
import com.rag.rag.application.usecase.DocumentProcessingRequestNotFoundException;
import com.rag.rag.application.usecase.DocumentProcessingRequestNotRetryableException;
import com.rag.rag.application.usecase.DocumentProcessingStatus;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestQuery;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestResult;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestUseCase;
import com.rag.rag.application.usecase.GetDocumentQuery;
import com.rag.rag.application.usecase.GetDocumentResult;
import com.rag.rag.application.usecase.GetDocumentUseCase;
import com.rag.rag.application.usecase.ProcessDocumentCommand;
import com.rag.rag.application.usecase.RegisterDocumentCommand;
import com.rag.rag.application.usecase.RegisterDocumentResult;
import com.rag.rag.application.usecase.RegisterDocumentUseCase;
import com.rag.rag.application.usecase.RequestDocumentProcessingUseCase;
import com.rag.rag.application.usecase.RetryDocumentProcessingRequestCommand;
import com.rag.rag.application.usecase.RetryDocumentProcessingRequestUseCase;
import com.rag.rag.domain.document.DocumentSourceType;
import com.rag.rag.domain.document.DocumentStatus;
import java.time.Instant;
import java.util.Map;
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

	@MockitoBean
	private GetDocumentUseCase getDocument;

	@MockitoBean
	private RequestDocumentProcessingUseCase requestDocumentProcessing;

	@MockitoBean
	private GetDocumentProcessingRequestUseCase getDocumentProcessingRequest;

	@MockitoBean
	private RetryDocumentProcessingRequestUseCase retryDocumentProcessingRequest;

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

	@Test
	void requiresAuthenticationWhenGettingDocument() throws Exception {
		mockMvc.perform(get(
			"/api/v1/workspaces/{workspaceId}/documents/{documentId}",
			UUID.randomUUID(),
			UUID.randomUUID()))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(getDocument);
	}

	@Test
	void getsDocumentForAuthenticatedUser() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		when(getDocument.execute(any(GetDocumentQuery.class)))
			.thenReturn(new GetDocumentResult(
				documentId,
				workspaceId,
				"Architecture Notes",
				DocumentSourceType.URL,
				"https://example.com/docs",
				"checksum-123",
				DocumentStatus.READY,
				Map.of("tag", "architecture")));

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}",
				workspaceId,
				documentId)
				.with(user("engineer")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.documentId").value(documentId.toString()))
			.andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
			.andExpect(jsonPath("$.title").value("Architecture Notes"))
			.andExpect(jsonPath("$.sourceType").value("URL"))
			.andExpect(jsonPath("$.sourceUri").value("https://example.com/docs"))
			.andExpect(jsonPath("$.checksum").value("checksum-123"))
			.andExpect(jsonPath("$.status").value("READY"))
			.andExpect(jsonPath("$.metadata.tag").value("architecture"));

		var query = ArgumentCaptor.forClass(GetDocumentQuery.class);
		verify(getDocument).execute(query.capture());
		assertEquals(workspaceId, query.getValue().workspaceId());
		assertEquals(documentId, query.getValue().documentId());
	}

	@Test
	void returnsProblemDetailWhenDocumentDoesNotExist() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		when(getDocument.execute(any(GetDocumentQuery.class)))
			.thenThrow(new DocumentNotFoundException());

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}",
				workspaceId,
				documentId)
				.with(user("engineer")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Document not found"))
			.andExpect(jsonPath("$.detail").value("document not found"));
	}

	@Test
	void requiresAuthenticationWhenRequestingDocumentProcessing() throws Exception {
		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing",
				UUID.randomUUID(),
				UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validProcessingRequest()))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(requestDocumentProcessing);
	}

	@Test
	void acceptsDocumentProcessingRequest() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var requestId = UUID.randomUUID();
		when(requestDocumentProcessing.execute(any(ProcessDocumentCommand.class))).thenReturn(requestId);

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing",
				workspaceId,
				documentId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validProcessingRequest()))
			.andExpect(status().isAccepted())
			.andExpect(header().string(
				"Location",
				"/api/v1/workspaces/%s/documents/%s/processing/%s"
					.formatted(workspaceId, documentId, requestId)))
			.andExpect(jsonPath("$.requestId").value(requestId.toString()))
			.andExpect(jsonPath("$.status").value("PENDING"));

		var command = ArgumentCaptor.forClass(ProcessDocumentCommand.class);
		verify(requestDocumentProcessing).execute(command.capture());
		assertEquals(workspaceId, command.getValue().workspaceId());
		assertEquals(documentId, command.getValue().documentId());
		assertEquals("Architecture content", command.getValue().content());
		assertEquals(256, command.getValue().maxTokens());
	}

	@Test
	void getsDocumentProcessingRequestStatus() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var requestId = UUID.randomUUID();
		var createdAt = Instant.parse("2026-07-15T04:00:00Z");
		when(getDocumentProcessingRequest.execute(any(GetDocumentProcessingRequestQuery.class)))
			.thenReturn(new GetDocumentProcessingRequestResult(
				requestId,
				workspaceId,
				documentId,
				DocumentProcessingStatus.PROCESSING,
				1,
				2,
				null,
				createdAt,
				createdAt.plusSeconds(10),
				createdAt.plusSeconds(2),
				null,
				null));

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing/{requestId}",
				workspaceId,
				documentId,
				requestId)
				.with(user("engineer")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requestId").value(requestId.toString()))
			.andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
			.andExpect(jsonPath("$.documentId").value(documentId.toString()))
			.andExpect(jsonPath("$.status").value("PROCESSING"))
			.andExpect(jsonPath("$.terminal").value(false))
			.andExpect(jsonPath("$.dispatchAttempts").value(1))
			.andExpect(jsonPath("$.processingAttempts").value(2))
			.andExpect(jsonPath("$.createdAt").value("2026-07-15T04:00:00Z"));

		var query = ArgumentCaptor.forClass(GetDocumentProcessingRequestQuery.class);
		verify(getDocumentProcessingRequest).execute(query.capture());
		assertEquals(workspaceId, query.getValue().workspaceId());
		assertEquals(documentId, query.getValue().documentId());
		assertEquals(requestId, query.getValue().requestId());
	}

	@Test
	void returnsNotFoundForUnknownProcessingRequest() throws Exception {
		when(getDocumentProcessingRequest.execute(any(GetDocumentProcessingRequestQuery.class)))
			.thenThrow(new DocumentProcessingRequestNotFoundException());

		mockMvc.perform(get(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing/{requestId}",
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID())
				.with(user("engineer")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Document processing request not found"))
			.andExpect(jsonPath("$.detail").value("document processing request not found"));
	}

	@Test
	void retriesFailedDocumentProcessingRequest() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var requestId = UUID.randomUUID();

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing/{requestId}/retry",
				workspaceId,
				documentId,
				requestId)
				.with(user("engineer")))
			.andExpect(status().isAccepted())
			.andExpect(header().string(
				"Location",
				"/api/v1/workspaces/%s/documents/%s/processing/%s"
					.formatted(workspaceId, documentId, requestId)))
			.andExpect(jsonPath("$.requestId").value(requestId.toString()))
			.andExpect(jsonPath("$.status").value("PENDING"));

		var command = ArgumentCaptor.forClass(RetryDocumentProcessingRequestCommand.class);
		verify(retryDocumentProcessingRequest).execute(command.capture());
		assertEquals(workspaceId, command.getValue().workspaceId());
		assertEquals(documentId, command.getValue().documentId());
		assertEquals(requestId, command.getValue().requestId());
	}

	@Test
	void returnsConflictWhenProcessingRequestCannotBeRetried() throws Exception {
		doThrow(new DocumentProcessingRequestNotRetryableException())
			.when(retryDocumentProcessingRequest)
			.execute(any(RetryDocumentProcessingRequestCommand.class));

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing/{requestId}/retry",
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID())
				.with(user("engineer")))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title")
				.value("Document processing request is not retryable"))
			.andExpect(jsonPath("$.detail")
				.value("only failed document processing requests can be retried"));
	}

	@Test
	void requiresAuthenticationWhenRetryingDocumentProcessingRequest() throws Exception {
		mockMvc.perform(post(
			"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing/{requestId}/retry",
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID()))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(retryDocumentProcessingRequest);
	}

	@Test
	void returnsProblemDetailForInvalidProcessingRequest() throws Exception {
		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing",
				UUID.randomUUID(),
				UUID.randomUUID())
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "content": " ",
					  "maxTokens": 0
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("content is required"));

		verifyNoInteractions(requestDocumentProcessing);
	}

	@Test
	void returnsNotFoundWhenRequestingProcessingForUnknownDocument() throws Exception {
		var workspaceId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		doThrow(new DocumentNotFoundException())
			.when(requestDocumentProcessing)
			.execute(any(ProcessDocumentCommand.class));

		mockMvc.perform(post(
				"/api/v1/workspaces/{workspaceId}/documents/{documentId}/processing",
				workspaceId,
				documentId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validProcessingRequest()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("document not found"));
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

	private String validProcessingRequest() {
		return """
			{
			  "content": "Architecture content",
			  "maxTokens": 256
			}
			""";
	}
}
