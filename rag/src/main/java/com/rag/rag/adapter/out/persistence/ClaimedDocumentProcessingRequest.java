package com.rag.rag.adapter.out.persistence;

import java.util.UUID;

public record ClaimedDocumentProcessingRequest(UUID requestId, int attempt) {
}
