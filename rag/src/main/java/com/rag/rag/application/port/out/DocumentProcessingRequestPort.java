package com.rag.rag.application.port.out;

import com.rag.rag.application.usecase.ProcessDocumentCommand;
import java.util.Optional;
import java.util.UUID;

public interface DocumentProcessingRequestPort {

	UUID create(ProcessDocumentCommand command);

	Optional<ProcessDocumentCommand> findCommandById(UUID requestId);

	void markCompleted(UUID requestId);

}
