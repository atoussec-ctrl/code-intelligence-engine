package com.rag.rag.application.port.out;

import com.rag.rag.application.rag.RetrievalQuery;
import com.rag.rag.application.rag.RetrievedContext;
import java.util.List;

public interface VectorSearchPort {
    List<RetrievedContext> search(RetrievalQuery query);
}