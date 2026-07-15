package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.rag.AskQuestionUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/rag")
class RagController {

	private final AskQuestionUseCase askQuestion;

	RagController(AskQuestionUseCase askQuestion) {
		this.askQuestion = askQuestion;
	}

	@PostMapping("/answers")
	RagAnswerResponse answer(
		@PathVariable UUID workspaceId,
		@RequestBody AskQuestionRequest request) {
		return RagAnswerResponse.from(askQuestion.execute(request.toQuery(workspaceId)));
	}

}
