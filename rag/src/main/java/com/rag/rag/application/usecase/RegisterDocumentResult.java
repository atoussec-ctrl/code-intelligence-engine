package com.rag.rag.application.usecase;

import com.rag.rag.domain.document.DocumentStatus;
import java.util.UUID;

public record RegisterDocumentResult(UUID documentId, DocumentStatus status) {
}
