CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    title TEXT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_uri TEXT,
    checksum TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, id)
);

CREATE TABLE chunks (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, document_id, id),
    UNIQUE (workspace_id, document_id, chunk_index),
    CONSTRAINT fk_chunks_document
        FOREIGN KEY (workspace_id, document_id)
        REFERENCES documents (workspace_id, id)
        ON DELETE CASCADE,
    CONSTRAINT chk_chunks_chunk_index_non_negative CHECK (chunk_index >= 0),
    CONSTRAINT chk_chunks_token_count_positive CHECK (token_count > 0)
);

CREATE TABLE chunk_embeddings (
    chunk_id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    model TEXT NOT NULL,
    embedding vector NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chunk_embeddings_chunk
        FOREIGN KEY (workspace_id, document_id, chunk_id)
        REFERENCES chunks (workspace_id, document_id, id)
        ON DELETE CASCADE
);

CREATE INDEX idx_documents_workspace ON documents (workspace_id);
CREATE INDEX idx_chunks_workspace_document ON chunks (workspace_id, document_id);
CREATE INDEX idx_chunk_embeddings_workspace_document ON chunk_embeddings (workspace_id, document_id);
