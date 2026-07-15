package com.rag.rag.application.port.out;

import com.rag.rag.application.rag.RagAnswer;
import com.rag.rag.application.rag.RagPrompt;

public interface RagAnswerGeneratorPort {

	RagAnswer generate(RagPrompt prompt);

}
