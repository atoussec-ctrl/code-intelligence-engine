ALTER TABLE document_processing_requests
    RENAME COLUMN attempts TO dispatch_attempts;

ALTER TABLE document_processing_requests
    ADD COLUMN processing_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN failed_at TIMESTAMPTZ;

ALTER TABLE document_processing_requests
    DROP CONSTRAINT chk_processing_request_attempts,
    DROP CONSTRAINT chk_processing_request_status;

ALTER TABLE document_processing_requests
    ADD CONSTRAINT chk_processing_request_dispatch_attempts
        CHECK (dispatch_attempts >= 0),
    ADD CONSTRAINT chk_processing_request_processing_attempts
        CHECK (processing_attempts >= 0),
    ADD CONSTRAINT chk_processing_request_status CHECK (
        status IN (
            'PENDING',
            'DISPATCHING',
            'PUBLISHED',
            'PROCESSING',
            'COMPLETED',
            'FAILED'));

DROP INDEX idx_processing_requests_dispatch;

CREATE INDEX idx_processing_requests_dispatch
    ON document_processing_requests (status, available_at, lease_until, created_at)
    WHERE status IN ('PENDING', 'DISPATCHING', 'PROCESSING');
