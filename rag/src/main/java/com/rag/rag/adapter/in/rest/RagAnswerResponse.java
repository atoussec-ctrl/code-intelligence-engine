package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.rag.RagAnswer;
import com.rag.rag.application.rag.RagCitation;
import java.util.List;
import java.util.UUID;

record RagAnswerResponse(String answer, List<RagCitationResponse> citations) {

	static RagAnswerResponse from(RagAnswer answer) {
		return new RagAnswerResponse(
			answer.answer(),
			answer.citations().stream().map(RagCitationResponse::from).toList());
	}

}

record RagCitationResponse(UUID chunkId, UUID documentId, String sourceTitle) {

	static RagCitationResponse from(RagCitation citation) {
		return new RagCitationResponse(
			citation.chunkId(),
			citation.documentId(),
			citation.sourceTitle());
	}

}
