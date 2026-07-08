package com.rag.rag.application.rag;

import java.util.List;
import java.util.Objects;

public record RagOutputValidationResult(boolean valid, List<String> errors) {
    public RagOutputValidationResult {
        errors = List.copyOf(Objects.requireNonNull(errors, "errors are required"));
        if (valid && !errors.isEmpty()) {
            throw new IllegalArgumentException("valid result cannot contain errors");
        }
    }

    public static RagOutputValidationResult success() {
        return new RagOutputValidationResult(true, List.of());
    }

    public static RagOutputValidationResult invalid(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("invalid result requires errors");
        }
        return new RagOutputValidationResult(false, errors);
    }
}