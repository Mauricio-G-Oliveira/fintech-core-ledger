package com.fintech.ledger.domain.exception;

/**
 * Base abstract domain exception for Core Ledger business violations.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
