package com.fintech.ledger.domain.exception;

/**
 * Thrown when attempting an arithmetic or entry operation with mismatched currencies.
 */
public class InvalidCurrencyException extends DomainException {

    public InvalidCurrencyException(String message) {
        super(message);
    }
}
