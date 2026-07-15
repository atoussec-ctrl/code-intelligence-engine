package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.rag.InvalidRagAnswerException;
import com.rag.rag.application.rag.RagAnswerGenerationException;
import com.rag.rag.application.usecase.DocumentNotFoundException;
import com.rag.rag.application.usecase.DocumentProcessingRequestNotFoundException;
import com.rag.rag.application.usecase.DocumentProcessingRequestNotRetryableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class RestExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
		return badRequest(exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableBody() {
		return badRequest("request body is malformed or contains unsupported values");
	}

	@ExceptionHandler(DocumentNotFoundException.class)
	ProblemDetail handleDocumentNotFound(DocumentNotFoundException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Document not found");
		return problem;
	}

	@ExceptionHandler(DocumentProcessingRequestNotFoundException.class)
	ProblemDetail handleProcessingRequestNotFound(DocumentProcessingRequestNotFoundException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Document processing request not found");
		return problem;
	}

	@ExceptionHandler(DocumentProcessingRequestNotRetryableException.class)
	ProblemDetail handleProcessingRequestNotRetryable(
		DocumentProcessingRequestNotRetryableException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Document processing request is not retryable");
		return problem;
	}

	@ExceptionHandler(InvalidRagAnswerException.class)
	ProblemDetail handleInvalidRagAnswer(InvalidRagAnswerException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
		problem.setTitle("Invalid AI response");
		problem.setProperty("errors", exception.errors());
		return problem;
	}

	@ExceptionHandler(RagAnswerGenerationException.class)
	ProblemDetail handleRagAnswerGenerationFailure(RagAnswerGenerationException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
		problem.setTitle("AI generation failed");
		return problem;
	}

	private ProblemDetail badRequest(String detail) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}
}
