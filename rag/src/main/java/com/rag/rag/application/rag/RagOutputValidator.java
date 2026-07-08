package com.rag.rag.application.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RagOutputValidator {
    public RagOutputValidationResult validate(RagAnswer answer, List<RetrievedContext> contexts) {
        Objects.requireNonNull(answer, "answer is required");
        List<RetrievedContext> safeContexts = List.copyOf(Objects.requireNonNull(contexts, "contexts are required"));
        List<String> errors = new ArrayList<>();

        if (!safeContexts.isEmpty() && answer.citations().isEmpty()) {
            errors.add("citation is required when context is used");
        }

        Map<UUID, RetrievedContext> contextsByChunk = contextsByChunk(safeContexts);
        for (RagCitation citation : answer.citations()) {
            RetrievedContext context = contextsByChunk.get(citation.chunkId());
            if (context == null) {
                errors.add("citation references unretrieved chunk");
                continue;
            }
            if (!context.documentId().equals(citation.documentId())) {
                errors.add("citation document does not match retrieved chunk");
            }
        }

        if (attemptsSystemPromptLeakage(answer.answer())) {
            errors.add("answer attempts to reveal system prompt");
        }

        if (errors.isEmpty()) {
            return RagOutputValidationResult.success();
        }
        return RagOutputValidationResult.invalid(errors);
    }

    private static Map<UUID, RetrievedContext> contextsByChunk(List<RetrievedContext> contexts) {
        Map<UUID, RetrievedContext> byChunk = new HashMap<>();
        for (RetrievedContext context : contexts) {
            byChunk.put(context.chunkId(), context);
        }
        return byChunk;
    }

    private static boolean attemptsSystemPromptLeakage(String answer) {
        String normalized = answer.toLowerCase(Locale.ROOT);
        return normalized.contains("system prompt:")
                || normalized.contains("developer message:")
                || normalized.contains("hidden instructions:")
                || normalized.contains("internal policy:");
    }
}