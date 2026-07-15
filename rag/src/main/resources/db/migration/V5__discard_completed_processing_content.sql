ALTER TABLE document_processing_requests
    ALTER COLUMN content DROP NOT NULL,
    DROP CONSTRAINT chk_processing_request_content;

UPDATE document_processing_requests
SET content = NULL
WHERE status = 'COMPLETED';

ALTER TABLE document_processing_requests
    ADD CONSTRAINT chk_processing_request_content CHECK (
        (status = 'COMPLETED' AND content IS NULL)
        OR (
            status <> 'COMPLETED'
            AND content IS NOT NULL
            AND btrim(content) <> ''
        )
    );
