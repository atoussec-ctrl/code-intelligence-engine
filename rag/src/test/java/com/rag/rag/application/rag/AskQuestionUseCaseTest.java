package com.rag.rag.application.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rag.rag.application.port.out.RagAnswerGeneratorPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AskQuestionUseCaseTest {

	@Test
	void retrievesBuildsGeneratesAndValidatesGroundedAnswer() {
		var retrieveContext = mock(RetrieveContextUseCase.class);
		var promptBuilder = mock(RagPromptBuilder.class);
		var answerGenerator = mock(RagAnswerGeneratorPort.class);
		var outputValidator = mock(RagOutputValidator.class);
		var query = query();
		var retrievalQuery = new RetrievalQuery(
			query.workspaceId(), query.question(), query.topK());
		var context = context(query.workspaceId());
		var contexts = List.of(context);
		var prompt = prompt(query.question());
		var answer = answer(context);
		when(retrieveContext.execute(retrievalQuery)).thenReturn(contexts);
		when(promptBuilder.build(query.question(), contexts)).thenReturn(prompt);
		when(answerGenerator.generate(prompt)).thenReturn(answer);
		when(outputValidator.validate(answer, contexts))
			.thenReturn(RagOutputValidationResult.success());
		var useCase = new AskQuestionUseCase(
			retrieveContext, promptBuilder, answerGenerator, outputValidator);

		var result = useCase.execute(query);

		assertSame(answer, result);
		verify(retrieveContext).execute(retrievalQuery);
		verify(promptBuilder).build(query.question(), contexts);
		verify(answerGenerator).generate(prompt);
		verify(outputValidator).validate(answer, contexts);
	}

	@Test
	void rejectsAnswerThatFailsGroundingValidation() {
		var retrieveContext = mock(RetrieveContextUseCase.class);
		var promptBuilder = mock(RagPromptBuilder.class);
		var answerGenerator = mock(RagAnswerGeneratorPort.class);
		var outputValidator = mock(RagOutputValidator.class);
		var query = query();
		var contexts = List.of(context(query.workspaceId()));
		var prompt = prompt(query.question());
		var answer = new RagAnswer("Unsupported answer", List.of());
		var errors = List.of("citation is required when context is used");
		when(retrieveContext.execute(new RetrievalQuery(
			query.workspaceId(), query.question(), query.topK()))).thenReturn(contexts);
		when(promptBuilder.build(query.question(), contexts)).thenReturn(prompt);
		when(answerGenerator.generate(prompt)).thenReturn(answer);
		when(outputValidator.validate(answer, contexts))
			.thenReturn(RagOutputValidationResult.invalid(errors));
		var useCase = new AskQuestionUseCase(
			retrieveContext, promptBuilder, answerGenerator, outputValidator);

		var exception = assertThrows(InvalidRagAnswerException.class, () -> useCase.execute(query));

		assertEquals("AI model returned an answer that failed grounding validation", exception.getMessage());
		assertEquals(errors, exception.errors());
		assertThrows(UnsupportedOperationException.class, () -> exception.errors().add("changed"));
	}

	@Test
	void rejectsMissingGeneratedAnswer() {
		var retrieveContext = mock(RetrieveContextUseCase.class);
		var promptBuilder = mock(RagPromptBuilder.class);
		var answerGenerator = mock(RagAnswerGeneratorPort.class);
		var outputValidator = mock(RagOutputValidator.class);
		var query = query();
		var contexts = List.of(context(query.workspaceId()));
		var prompt = prompt(query.question());
		when(retrieveContext.execute(new RetrievalQuery(
			query.workspaceId(), query.question(), query.topK()))).thenReturn(contexts);
		when(promptBuilder.build(query.question(), contexts)).thenReturn(prompt);
		when(answerGenerator.generate(prompt)).thenReturn(null);
		var useCase = new AskQuestionUseCase(
			retrieveContext, promptBuilder, answerGenerator, outputValidator);

		assertEquals(
			"generated answer is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(query)).getMessage());
	}

	@Test
	void acceptsAnswerWithoutCitationsWhenNoEvidenceIsAvailable() {
		var retrieveContext = mock(RetrieveContextUseCase.class);
		var answerGenerator = mock(RagAnswerGeneratorPort.class);
		var query = query();
		var retrievalQuery = new RetrievalQuery(
			query.workspaceId(), query.question(), query.topK());
		when(retrieveContext.execute(retrievalQuery)).thenReturn(List.of());
		var answer = new RagAnswer("The available knowledge cannot answer the question.", List.of());
		when(answerGenerator.generate(any(RagPrompt.class)))
			.thenReturn(answer);
		var useCase = new AskQuestionUseCase(
			retrieveContext,
			new RagPromptBuilder(),
			answerGenerator,
			new RagOutputValidator());

		var result = useCase.execute(query);

		assertSame(answer, result);
	}

	@Test
	void validatesDependenciesQueryAndValidationErrors() {
		var retrieveContext = mock(RetrieveContextUseCase.class);
		var promptBuilder = mock(RagPromptBuilder.class);
		var answerGenerator = mock(RagAnswerGeneratorPort.class);
		var outputValidator = mock(RagOutputValidator.class);

		assertEquals(
			"retrieve context is required",
			assertThrows(
				NullPointerException.class,
				() -> new AskQuestionUseCase(null, promptBuilder, answerGenerator, outputValidator))
				.getMessage());
		assertEquals(
			"prompt builder is required",
			assertThrows(
				NullPointerException.class,
				() -> new AskQuestionUseCase(retrieveContext, null, answerGenerator, outputValidator))
				.getMessage());
		assertEquals(
			"answer generator is required",
			assertThrows(
				NullPointerException.class,
				() -> new AskQuestionUseCase(retrieveContext, promptBuilder, null, outputValidator))
				.getMessage());
		assertEquals(
			"output validator is required",
			assertThrows(
				NullPointerException.class,
				() -> new AskQuestionUseCase(retrieveContext, promptBuilder, answerGenerator, null))
				.getMessage());
		var useCase = new AskQuestionUseCase(
			retrieveContext, promptBuilder, answerGenerator, outputValidator);
		assertEquals(
			"query is required",
			assertThrows(NullPointerException.class, () -> useCase.execute(null)).getMessage());
		assertEquals(
			"errors are required",
			assertThrows(NullPointerException.class, () -> new InvalidRagAnswerException(null))
				.getMessage());
		assertEquals(
			"at least one validation error is required",
			assertThrows(IllegalArgumentException.class, () -> new InvalidRagAnswerException(List.of()))
				.getMessage());
	}

	@Test
	void validatesQuestionScopeAndRetrievalLimit() {
		var workspaceId = UUID.randomUUID();

		assertEquals(
			"workspaceId is required",
			assertThrows(
				NullPointerException.class,
				() -> new AskQuestionQuery(null, "question", 5)).getMessage());
		assertEquals(
			"question is required",
			assertThrows(
				IllegalArgumentException.class,
				() -> new AskQuestionQuery(workspaceId, " ", 5)).getMessage());
		assertEquals(
			"question must not exceed 2000 characters",
			assertThrows(
				IllegalArgumentException.class,
				() -> new AskQuestionQuery(workspaceId, "a".repeat(2001), 5)).getMessage());
		assertEquals(
			"topK must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> new AskQuestionQuery(workspaceId, "question", 0)).getMessage());
		assertEquals(
			"topK must not exceed 20",
			assertThrows(
				IllegalArgumentException.class,
				() -> new AskQuestionQuery(workspaceId, "question", 21)).getMessage());
		assertEquals("question", new AskQuestionQuery(workspaceId, " question ", 5).question());
	}

	private AskQuestionQuery query() {
		return new AskQuestionQuery(UUID.randomUUID(), "How does the architecture work?", 5);
	}

	private RetrievedContext context(UUID workspaceId) {
		return new RetrievedContext(
			workspaceId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			"Architecture Notes",
			"Ports isolate application policy.",
			0.91);
	}

	private RagPrompt prompt(String question) {
		return new RagPrompt("system", question, "context", "rules", "format");
	}

	private RagAnswer answer(RetrievedContext context) {
		return new RagAnswer(
			"Ports isolate application policy.",
			List.of(new RagCitation(
				context.chunkId(), context.documentId(), context.sourceTitle())));
	}

}
