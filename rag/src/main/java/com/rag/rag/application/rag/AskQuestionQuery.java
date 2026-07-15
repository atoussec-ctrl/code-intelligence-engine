package com.rag.rag.application.rag;

import java.util.Objects;
import java.util.UUID;

public record AskQuestionQuery(UUID workspaceId, String question, int topK) {

	private static final int MAX_QUESTION_LENGTH = 2000;
	private static final int MAX_TOP_K = 20;

	public AskQuestionQuery {
		workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
		if (question == null || question.isBlank()) {
			throw new IllegalArgumentException("question is required");
		}
		question = question.trim();
		if (question.length() > MAX_QUESTION_LENGTH) {
			throw new IllegalArgumentException("question must not exceed 2000 characters");
		}
		if (topK <= 0) {
			throw new IllegalArgumentException("topK must be positive");
		}
		if (topK > MAX_TOP_K) {
			throw new IllegalArgumentException("topK must not exceed 20");
		}
	}

}
