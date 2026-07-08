package com.rag.rag.application.usecase;

import java.util.UUID;

public record ProcessDocumentResult(UUID documentId, int chunksCreated) {
}