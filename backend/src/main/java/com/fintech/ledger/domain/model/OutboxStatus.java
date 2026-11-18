package com.fintech.ledger.domain.model;

/**
 * Lifecycle state of an outbox event in the Transactional Outbox Pattern.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
