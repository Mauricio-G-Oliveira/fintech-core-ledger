package com.fintech.ledger.domain.exception;

/**
 * Thrown when an operation targets an account that is frozen or restricted.
 */
public class InactiveAccountException extends DomainException {

    public InactiveAccountException(String message) {
        super(message);
    }
}
