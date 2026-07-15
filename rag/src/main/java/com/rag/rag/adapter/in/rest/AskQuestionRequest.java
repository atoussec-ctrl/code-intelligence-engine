package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.rag.AskQuestionQuery;
import java.util.UUID;

record AskQuestionRequest(String question, Integer topK) {

	private static final int DEFAULT_TOP_K = 5;

	AskQuestionQuery toQuery(UUID workspaceId) {
		return new AskQuestionQuery(
			workspaceId,
			question,
			topK == null ? DEFAULT_TOP_K : topK);
	}

}
