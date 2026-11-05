package com.fintech.ledger.application.exception;

/**
 * Thrown when a requested transaction is not found in the immutable journal.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
