package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.RegisterDocumentUseCase;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/documents")
class DocumentController {

	private final RegisterDocumentUseCase registerDocument;

	DocumentController(RegisterDocumentUseCase registerDocument) {
		this.registerDocument = registerDocument;
	}

	@PostMapping
	ResponseEntity<RegisterDocumentResponse> register(
		@PathVariable UUID workspaceId,
		@RequestBody RegisterDocumentRequest request) {
		var result = registerDocument.execute(request.toCommand(workspaceId));
		var location = URI.create("/api/v1/workspaces/%s/documents/%s"
			.formatted(workspaceId, result.documentId()));
		return ResponseEntity.created(location)
			.body(RegisterDocumentResponse.from(result));
	}
}
