package com.fintech.ledger.domain.exception;

/**
 * Thrown when a financial transaction violates the fundamental Double-Entry Bookkeeping invariant:
 * Sum(Debits) != Sum(Credits).
 */
public class UnbalancedTransactionException extends DomainException {

    public UnbalancedTransactionException(String message) {
        super(message);
    }
}
