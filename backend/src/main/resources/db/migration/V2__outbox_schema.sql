-- ==============================================================================
-- V2__outbox_schema.sql
-- Transactional Outbox Pattern for Asynchronous Event Dispatching
-- Guarantees At-Least-Once Delivery to external webhooks without dual-write issues
-- ==============================================================================

CREATE TABLE ledger.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_pending ON ledger.outbox_events(status, next_retry_at);
CREATE INDEX idx_outbox_aggregate ON ledger.outbox_events(aggregate_type, aggregate_id);
