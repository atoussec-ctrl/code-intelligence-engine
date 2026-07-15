package com.rag.rag.adapter.in.rest;

import com.rag.rag.application.usecase.GetDocumentQuery;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestQuery;
import com.rag.rag.application.usecase.GetDocumentProcessingRequestUseCase;
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
	private final GetDocumentProcessingRequestUseCase getDocumentProcessingRequest;
	private final RequestDocumentProcessingUseCase requestDocumentProcessing;

	DocumentController(
		RegisterDocumentUseCase registerDocument,
		GetDocumentUseCase getDocument,
		GetDocumentProcessingRequestUseCase getDocumentProcessingRequest,
		RequestDocumentProcessingUseCase requestDocumentProcessing) {
		this.registerDocument = registerDocument;
		this.getDocument = getDocument;
		this.getDocumentProcessingRequest = getDocumentProcessingRequest;
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
	ResponseEntity<DocumentProcessingAcceptedResponse> process(
		@PathVariable UUID workspaceId,
		@PathVariable UUID documentId,
		@RequestBody ProcessDocumentRequest request) {
		var requestId = requestDocumentProcessing.execute(request.toCommand(workspaceId, documentId));
		var location = processingLocation(workspaceId, documentId, requestId);
		return ResponseEntity.accepted()
			.location(location)
			.body(DocumentProcessingAcceptedResponse.pending(requestId));
	}

	@GetMapping("/{documentId}/processing/{requestId}")
	DocumentProcessingResponse getProcessingRequest(
		@PathVariable UUID workspaceId,
		@PathVariable UUID documentId,
		@PathVariable UUID requestId) {
		return DocumentProcessingResponse.from(getDocumentProcessingRequest.execute(
			new GetDocumentProcessingRequestQuery(workspaceId, documentId, requestId)));
	}

	private static URI processingLocation(UUID workspaceId, UUID documentId, UUID requestId) {
		return URI.create("/api/v1/workspaces/%s/documents/%s/processing/%s"
			.formatted(workspaceId, documentId, requestId));
	}
}
