package com.rag.rag.application.port.out;

import com.rag.rag.application.usecase.ProcessDocumentCommand;

public interface DocumentProcessingQueuePort {

	void enqueue(ProcessDocumentCommand command);

}
