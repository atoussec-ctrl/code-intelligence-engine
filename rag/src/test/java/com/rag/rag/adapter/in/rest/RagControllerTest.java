package com.rag.rag.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rag.rag.application.rag.AskQuestionQuery;
import com.rag.rag.application.rag.AskQuestionUseCase;
import com.rag.rag.application.rag.InvalidRagAnswerException;
import com.rag.rag.application.rag.RagAnswer;
import com.rag.rag.application.rag.RagAnswerGenerationException;
import com.rag.rag.application.rag.RagCitation;
import java.util.List;
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
class RagControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AskQuestionUseCase askQuestion;

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(askQuestion);
	}

	@Test
	void answersQuestionWithGroundedCitations() throws Exception {
		var workspaceId = UUID.randomUUID();
		var chunkId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		when(askQuestion.execute(any(AskQuestionQuery.class))).thenReturn(new RagAnswer(
			"Ports isolate application policy.",
			List.of(new RagCitation(chunkId, documentId, "Architecture Notes"))));

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", workspaceId)
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.answer").value("Ports isolate application policy."))
			.andExpect(jsonPath("$.citations[0].chunkId").value(chunkId.toString()))
			.andExpect(jsonPath("$.citations[0].documentId").value(documentId.toString()))
			.andExpect(jsonPath("$.citations[0].sourceTitle").value("Architecture Notes"));

		var query = ArgumentCaptor.forClass(AskQuestionQuery.class);
		verify(askQuestion).execute(query.capture());
		assertEquals(workspaceId, query.getValue().workspaceId());
		assertEquals("How does the architecture work?", query.getValue().question());
		assertEquals(3, query.getValue().topK());
	}

	@Test
	void usesDefaultRetrievalLimit() throws Exception {
		when(askQuestion.execute(any(AskQuestionQuery.class)))
			.thenReturn(new RagAnswer("Knowledge is insufficient.", List.of()));

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", UUID.randomUUID())
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question": "Unknown question"
					}
					"""))
			.andExpect(status().isOk());

		var query = ArgumentCaptor.forClass(AskQuestionQuery.class);
		verify(askQuestion).execute(query.capture());
		assertEquals(5, query.getValue().topK());
	}

	@Test
	void rejectsInvalidQuestionBeforeCallingUseCase() throws Exception {
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", UUID.randomUUID())
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question": " ",
					  "topK": 0
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.detail").value("question is required"));

		verifyNoInteractions(askQuestion);
	}

	@Test
	void returnsBadGatewayWhenModelAnswerIsNotGrounded() throws Exception {
		doThrow(new InvalidRagAnswerException(
			List.of("citation references unretrieved chunk")))
			.when(askQuestion)
			.execute(any(AskQuestionQuery.class));

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", UUID.randomUUID())
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isBadGateway())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid AI response"))
			.andExpect(jsonPath("$.detail")
				.value("AI model returned an answer that failed grounding validation"))
			.andExpect(jsonPath("$.errors[0]").value("citation references unretrieved chunk"));
	}

	@Test
	void returnsBadGatewayWhenModelCannotProduceStructuredAnswer() throws Exception {
		doThrow(new RagAnswerGenerationException(new IllegalStateException("provider unavailable")))
			.when(askQuestion)
			.execute(any(AskQuestionQuery.class));

		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/rag/answers", UUID.randomUUID())
				.with(user("engineer"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isBadGateway())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("AI generation failed"))
			.andExpect(jsonPath("$.detail")
				.value("AI model failed to produce a structured answer"));
	}

	private String validRequest() {
		return """
			{
			  "question": "How does the architecture work?",
			  "topK": 3
			}
			""";
	}

}
