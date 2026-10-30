package com.fintech.ledger.domain.exception;

/**
 * Thrown when an account does not have sufficient balance to satisfy a debit leg.
 */
public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
