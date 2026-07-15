package com.rag.rag.application.rag;

import com.rag.rag.application.port.out.RagAnswerGeneratorPort;
import java.util.Objects;

public class AskQuestionUseCase {

	private final RetrieveContextUseCase retrieveContext;
	private final RagPromptBuilder promptBuilder;
	private final RagAnswerGeneratorPort answerGenerator;
	private final RagOutputValidator outputValidator;

	public AskQuestionUseCase(
		RetrieveContextUseCase retrieveContext,
		RagPromptBuilder promptBuilder,
		RagAnswerGeneratorPort answerGenerator,
		RagOutputValidator outputValidator) {
		this.retrieveContext = Objects.requireNonNull(retrieveContext, "retrieve context is required");
		this.promptBuilder = Objects.requireNonNull(promptBuilder, "prompt builder is required");
		this.answerGenerator = Objects.requireNonNull(answerGenerator, "answer generator is required");
		this.outputValidator = Objects.requireNonNull(outputValidator, "output validator is required");
	}

	public RagAnswer execute(AskQuestionQuery query) {
		Objects.requireNonNull(query, "query is required");
		var contexts = retrieveContext.execute(
			new RetrievalQuery(query.workspaceId(), query.question(), query.topK()));
		var prompt = promptBuilder.build(query.question(), contexts);
		var answer = Objects.requireNonNull(
			answerGenerator.generate(prompt),
			"generated answer is required");
		var validation = outputValidator.validate(answer, contexts);
		if (!validation.valid()) {
			throw new InvalidRagAnswerException(validation.errors());
		}
		return answer;
	}

}
