package com.fintech.ledger.application.exception;

import java.util.UUID;

/**
 * Thrown when an account requested for settlement does not exist in the ledger.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found with ID: " + accountId);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
