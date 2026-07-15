package com.rag.rag.application.rag;

import java.util.List;
import java.util.Objects;

public class InvalidRagAnswerException extends RuntimeException {

	private final List<String> errors;

	public InvalidRagAnswerException(List<String> errors) {
		super("AI model returned an answer that failed grounding validation");
		this.errors = List.copyOf(Objects.requireNonNull(errors, "errors are required"));
		if (this.errors.isEmpty()) {
			throw new IllegalArgumentException("at least one validation error is required");
		}
	}

	public List<String> errors() {
		return errors;
	}

}
