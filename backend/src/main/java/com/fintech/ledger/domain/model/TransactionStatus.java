package com.fintech.ledger.domain.model;

/**
 * Execution and persistence state of a ledger transaction.
 */
public enum TransactionStatus {
    COMMITTED,
    REJECTED_IDEMPOTENT,
    REJECTED_INSUFFICIENT_FUNDS,
    REJECTED_LOCK_TIMEOUT
}
