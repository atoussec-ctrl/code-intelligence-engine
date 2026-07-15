package com.rag.rag.application.rag;

public class RagAnswerGenerationException extends RuntimeException {

	public RagAnswerGenerationException(Throwable cause) {
		super("AI model failed to produce a structured answer", cause);
	}

}
