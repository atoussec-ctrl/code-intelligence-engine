CREATE TABLE document_processing_requests (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    content TEXT NOT NULL,
    max_tokens INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lease_until TIMESTAMPTZ,
    last_error TEXT,
    published_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_processing_request_document
        FOREIGN KEY (workspace_id, document_id)
        REFERENCES documents (workspace_id, id)
        ON DELETE CASCADE,
    CONSTRAINT chk_processing_request_content CHECK (btrim(content) <> ''),
    CONSTRAINT chk_processing_request_max_tokens CHECK (max_tokens > 0),
    CONSTRAINT chk_processing_request_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_processing_request_status CHECK (
        status IN ('PENDING', 'DISPATCHING', 'PUBLISHED', 'COMPLETED'))
);

CREATE INDEX idx_processing_requests_dispatch
    ON document_processing_requests (status, available_at, lease_until, created_at)
    WHERE status IN ('PENDING', 'DISPATCHING');
