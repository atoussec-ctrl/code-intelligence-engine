package com.rag.rag.adapter.out.generation;

import com.rag.rag.application.port.out.RagAnswerGeneratorPort;
import com.rag.rag.application.rag.RagAnswer;
import com.rag.rag.application.rag.RagAnswerGenerationException;
import com.rag.rag.application.rag.RagCitation;
import com.rag.rag.application.rag.RagPrompt;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;

public class SpringAiRagAnswerGeneratorAdapter implements RagAnswerGeneratorPort {

	private final ChatClient chatClient;

	public SpringAiRagAnswerGeneratorAdapter(ChatClient chatClient) {
		this.chatClient = Objects.requireNonNull(chatClient, "chat client is required");
	}

	@Override
	public RagAnswer generate(RagPrompt prompt) {
		Objects.requireNonNull(prompt, "prompt is required");
		try {
			var generated = Objects.requireNonNull(
				chatClient.prompt()
					.system(systemMessage(prompt))
					.user(userMessage(prompt))
					.call()
					.entity(GeneratedRagAnswer.class, specification -> specification.validateSchema()),
				"chat model response is required");
			return generated.toDomain();
		}
		catch (RuntimeException exception) {
			throw new RagAnswerGenerationException(exception);
		}
	}

	private static String systemMessage(RagPrompt prompt) {
		return String.join(
			"\n\n",
			prompt.systemInstructions(),
			prompt.citationRules(),
			prompt.responseFormat());
	}

	private static String userMessage(RagPrompt prompt) {
		return """
			USER QUESTION
			%s

			RETRIEVED EVIDENCE
			%s
			""".formatted(prompt.userQuestion(), prompt.retrievedContext()).trim();
	}

}

record GeneratedRagAnswer(String answer, List<GeneratedRagCitation> citations) {

	RagAnswer toDomain() {
		var safeCitations = List.copyOf(
			Objects.requireNonNull(citations, "generated citations are required"));
		return new RagAnswer(
			answer,
			safeCitations.stream().map(GeneratedRagCitation::toDomain).toList());
	}

}

record GeneratedRagCitation(UUID chunkId, UUID documentId, String sourceTitle) {

	RagCitation toDomain() {
		return new RagCitation(chunkId, documentId, sourceTitle);
	}

}
