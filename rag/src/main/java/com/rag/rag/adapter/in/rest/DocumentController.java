package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.GetDocumentQuery;
import com.rag.rag.application.usecase.GetDocumentUseCase;
import com.rag.rag.application.usecase.RequestDocumentProcessingUseCase;
import com.rag.rag.application.usecase.RegisterDocumentUseCase;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/documents")
class DocumentController {

	private final RegisterDocumentUseCase registerDocument;
	private final GetDocumentUseCase getDocument;
	private final RequestDocumentProcessingUseCase requestDocumentProcessing;

	DocumentController(
		RegisterDocumentUseCase registerDocument,
		GetDocumentUseCase getDocument,
		RequestDocumentProcessingUseCase requestDocumentProcessing) {
		this.registerDocument = registerDocument;
		this.getDocument = getDocument;
		this.requestDocumentProcessing = requestDocumentProcessing;
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

	@GetMapping("/{documentId}")
	DocumentResponse get(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
		return DocumentResponse.from(getDocument.execute(new GetDocumentQuery(workspaceId, documentId)));
	}

	@PostMapping("/{documentId}/processing")
	ResponseEntity<Void> process(
		@PathVariable UUID workspaceId,
		@PathVariable UUID documentId,
		@RequestBody ProcessDocumentRequest request) {
		requestDocumentProcessing.execute(request.toCommand(workspaceId, documentId));
		var location = URI.create("/api/v1/workspaces/%s/documents/%s"
			.formatted(workspaceId, documentId));
		return ResponseEntity.accepted().location(location).build();
	}
}
