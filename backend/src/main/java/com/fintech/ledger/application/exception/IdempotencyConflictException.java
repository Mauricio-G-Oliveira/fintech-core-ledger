package com.fintech.ledger.application.exception;

/**
 * Thrown when an idempotency key is reused with a different request payload (tampering/conflict).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
