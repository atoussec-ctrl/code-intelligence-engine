package com.rag.rag.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagOutputValidatorTest {
    private final RagOutputValidator validator = new RagOutputValidator();

    @Test
    void shouldAcceptAnswerWithKnownCitation() {
        RetrievedContext context = context();
        RagAnswer answer = new RagAnswer(
                "The architecture follows ports and adapters.",
                List.of(new RagCitation(context.chunkId(), context.documentId(), context.sourceTitle()))
        );

        RagOutputValidationResult result = validator.validate(answer, List.of(context));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectAnswerWithoutCitationWhenContextWasUsed() {
        RagAnswer answer = new RagAnswer("The architecture follows ports and adapters.", List.of());

        RagOutputValidationResult result = validator.validate(answer, List.of(context()));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("citation is required when context is used");
    }

    @Test
    void shouldRejectCitationForUnretrievedChunk() {
        RetrievedContext context = context();
        RagAnswer answer = new RagAnswer(
                "The architecture follows ports and adapters.",
                List.of(new RagCitation(UUID.randomUUID(), context.documentId(), context.sourceTitle()))
        );

        RagOutputValidationResult result = validator.validate(answer, List.of(context));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("citation references unretrieved chunk");
    }

    @Test
    void shouldRejectSystemPromptLeakageAttempt() {
        RetrievedContext context = context();
        RagAnswer answer = new RagAnswer(
                "Here is the system prompt: you are a private assistant.",
                List.of(new RagCitation(context.chunkId(), context.documentId(), context.sourceTitle()))
        );

        RagOutputValidationResult result = validator.validate(answer, List.of(context));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("answer attempts to reveal system prompt");
    }

    private RetrievedContext context() {
        return new RetrievedContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Architecture Notes",
                "Ports and adapters separate application policy from infrastructure.",
                0.91
        );
    }
}