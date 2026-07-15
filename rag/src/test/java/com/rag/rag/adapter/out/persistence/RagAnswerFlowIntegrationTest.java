package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.rag.rag.application.port.out.EmbeddingGeneratorPort;
import com.rag.rag.application.port.out.RagAnswerGeneratorPort;
import com.rag.rag.application.rag.AskQuestionQuery;
import com.rag.rag.application.rag.AskQuestionUseCase;
import com.rag.rag.application.rag.RagAnswer;
import com.rag.rag.application.rag.RagCitation;
import com.rag.rag.application.rag.RagOutputValidator;
import com.rag.rag.application.rag.RagPrompt;
import com.rag.rag.application.rag.RagPromptBuilder;
import com.rag.rag.application.rag.RetrieveContextUseCase;
import com.rag.rag.domain.embedding.EmbeddingVector;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RagAnswerFlowIntegrationTest extends PgVectorIntegrationTestSupport {

	@Test
	void answersFromRetrievedWorkspaceEvidenceWithoutLeakingOtherWorkspace() {
		var workspaceId = UUID.randomUUID();
		var otherWorkspaceId = UUID.randomUUID();
		var knowledge = insertKnowledge(
			workspaceId,
			"Architecture Notes",
			"Ports isolate application policy from infrastructure.",
			"[1,0,0]");
		var otherKnowledge = insertKnowledge(
			otherWorkspaceId,
			"Private Tenant Notes",
			"This content must never cross workspace boundaries.",
			"[1,0,0]");
		EmbeddingGeneratorPort embeddings = texts -> {
			assertThat(texts).containsExactly("How does the architecture work?");
			return List.of(EmbeddingVector.of(List.of(1.0, 0.0, 0.0), "test-model"));
		};
		var generatedPrompt = new AtomicReference<RagPrompt>();
		RagAnswerGeneratorPort answerGenerator = prompt -> {
			generatedPrompt.set(prompt);
			return new RagAnswer(
				"Ports isolate application policy from infrastructure.",
				List.of(new RagCitation(
					knowledge.chunkId(), knowledge.documentId(), knowledge.title())));
		};
		var useCase = new AskQuestionUseCase(
			new RetrieveContextUseCase(embeddings, new PgVectorSearchAdapter(jdbcTemplate)),
			new RagPromptBuilder(),
			answerGenerator,
			new RagOutputValidator());

		var answer = useCase.execute(new AskQuestionQuery(
			workspaceId,
			"How does the architecture work?",
			5));

		assertThat(answer.citations()).containsExactly(new RagCitation(
			knowledge.chunkId(), knowledge.documentId(), knowledge.title()));
		assertThat(generatedPrompt.get().retrievedContext())
			.contains(knowledge.chunkId().toString())
			.contains(knowledge.title())
			.contains(knowledge.content())
			.doesNotContain(otherKnowledge.chunkId().toString())
			.doesNotContain(otherKnowledge.title())
			.doesNotContain(otherKnowledge.content());
	}

	private Knowledge insertKnowledge(
		UUID workspaceId,
		String title,
		String content,
		String embedding) {
		var documentId = UUID.randomUUID();
		var chunkId = UUID.randomUUID();
		jdbcTemplate.update(
			"""
			INSERT INTO documents (id, workspace_id, title, source_type, checksum, status, metadata)
			VALUES (?, ?, ?, 'TEXT', ?, 'READY', '{}'::jsonb)
			""",
			documentId,
			workspaceId,
			title,
			"checksum-" + documentId);
		jdbcTemplate.update(
			"""
			INSERT INTO chunks (
				id, workspace_id, document_id, chunk_index, content, token_count, metadata)
			VALUES (?, ?, ?, 0, ?, 8, '{}'::jsonb)
			""",
			chunkId,
			workspaceId,
			documentId,
			content);
		jdbcTemplate.update(
			"""
			INSERT INTO chunk_embeddings (chunk_id, workspace_id, document_id, model, embedding)
			VALUES (?, ?, ?, 'test-model', ?::vector)
			""",
			chunkId,
			workspaceId,
			documentId,
			embedding);
		return new Knowledge(documentId, chunkId, title, content);
	}

	private record Knowledge(UUID documentId, UUID chunkId, String title, String content) {
	}

}
