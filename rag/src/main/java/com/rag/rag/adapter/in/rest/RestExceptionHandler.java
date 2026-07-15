package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.DocumentNotFoundException;
import com.rag.rag.application.usecase.DocumentProcessingUnavailableException;
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

	@ExceptionHandler(DocumentProcessingUnavailableException.class)
	ProblemDetail handleProcessingUnavailable(DocumentProcessingUnavailableException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
		problem.setTitle("Document processing unavailable");
		return problem;
	}

	private ProblemDetail badRequest(String detail) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}
}
