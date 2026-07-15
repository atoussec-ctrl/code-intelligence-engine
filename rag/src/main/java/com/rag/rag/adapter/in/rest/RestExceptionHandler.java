package com.rag.rag.adapter.in.rest;

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

	private ProblemDetail badRequest(String detail) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}
}
