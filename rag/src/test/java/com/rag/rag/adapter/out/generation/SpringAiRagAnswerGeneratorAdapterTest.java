package com.rag.rag.adapter.out.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.rag.application.rag.RagPrompt;
import com.rag.rag.application.rag.RagAnswerGenerationException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

class SpringAiRagAnswerGeneratorAdapterTest {

	@Test
	@SuppressWarnings("unchecked")
	void generatesStructuredDomainAnswerWithSchemaValidation() {
		var chatClient = mock(ChatClient.class);
		var request = mock(ChatClient.ChatClientRequestSpec.class);
		var response = mock(ChatClient.CallResponseSpec.class);
		var chunkId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var generated = new GeneratedRagAnswer(
			"Ports isolate application policy.",
			List.of(new GeneratedRagCitation(chunkId, documentId, "Architecture Notes")));
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(any(String.class))).thenReturn(request);
		when(request.user(any(String.class))).thenReturn(request);
		when(request.call()).thenReturn(response);
		when(response.entity(eq(GeneratedRagAnswer.class), any(Consumer.class)))
			.thenAnswer(invocation -> {
				var specification = mock(ChatClient.EntityParamSpec.class);
				when(specification.validateSchema()).thenReturn(specification);
				Consumer<ChatClient.EntityParamSpec> customizer = invocation.getArgument(1);
				customizer.accept(specification);
				verify(specification).validateSchema();
				return generated;
			});
		var prompt = prompt();
		var adapter = new SpringAiRagAnswerGeneratorAdapter(chatClient);

		var answer = adapter.generate(prompt);

		assertEquals(generated.answer(), answer.answer());
		assertEquals(chunkId, answer.citations().getFirst().chunkId());
		assertEquals(documentId, answer.citations().getFirst().documentId());
		assertEquals("Architecture Notes", answer.citations().getFirst().sourceTitle());
		var systemMessage = ArgumentCaptor.forClass(String.class);
		var userMessage = ArgumentCaptor.forClass(String.class);
		verify(request).system(systemMessage.capture());
		verify(request).user(userMessage.capture());
		assertEquals("system\n\nrules\n\nformat", systemMessage.getValue());
		assertEquals(
			"USER QUESTION\nquestion\n\nRETRIEVED EVIDENCE\ncontext",
			userMessage.getValue());
	}

	@Test
	void validatesDependencyPromptAndModelResponse() {
		assertEquals(
			"chat client is required",
			assertThrows(
				NullPointerException.class,
				() -> new SpringAiRagAnswerGeneratorAdapter(null)).getMessage());
		var chatClient = mock(ChatClient.class);
		var adapter = new SpringAiRagAnswerGeneratorAdapter(chatClient);
		assertEquals(
			"prompt is required",
			assertThrows(NullPointerException.class, () -> adapter.generate(null)).getMessage());

		var request = mock(ChatClient.ChatClientRequestSpec.class);
		var response = mock(ChatClient.CallResponseSpec.class);
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(any(String.class))).thenReturn(request);
		when(request.user(any(String.class))).thenReturn(request);
		when(request.call()).thenReturn(response);
		when(response.entity(eq(GeneratedRagAnswer.class), any())).thenReturn(null);
		var exception = assertThrows(RagAnswerGenerationException.class, () -> adapter.generate(prompt()));
		assertEquals("AI model failed to produce a structured answer", exception.getMessage());
		assertEquals("chat model response is required", exception.getCause().getMessage());
	}

	@Test
	void rejectsGeneratedAnswerWithoutCitationsCollection() {
		var generated = new GeneratedRagAnswer("Answer", null);

		assertEquals(
			"generated citations are required",
			assertThrows(NullPointerException.class, generated::toDomain).getMessage());
	}

	@Test
	void convertsStructuredJsonResponseIntoAdapterSchema() {
		var chunkId = UUID.randomUUID();
		var documentId = UUID.randomUUID();
		var converter = new BeanOutputConverter<>(GeneratedRagAnswer.class);

		var generated = converter.convert("""
			{
			  "answer": "Ports isolate application policy.",
			  "citations": [
			    {
			      "chunkId": "%s",
			      "documentId": "%s",
			      "sourceTitle": "Architecture Notes"
			    }
			  ]
			}
			""".formatted(chunkId, documentId));

		assertEquals("Ports isolate application policy.", generated.answer());
		assertEquals(chunkId, generated.citations().getFirst().chunkId());
		assertEquals(documentId, generated.citations().getFirst().documentId());
		assertEquals("Architecture Notes", generated.citations().getFirst().sourceTitle());
	}

	private RagPrompt prompt() {
		return new RagPrompt("system", "question", "context", "rules", "format");
	}

}
