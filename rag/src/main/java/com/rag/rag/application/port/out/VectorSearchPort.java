package com.rag.rag.application.port.out;

import com.rag.rag.application.rag.RetrievedContext;
import com.rag.rag.application.rag.VectorSearchQuery;
import java.util.List;

public interface VectorSearchPort {
    List<RetrievedContext> search(VectorSearchQuery query);
}
