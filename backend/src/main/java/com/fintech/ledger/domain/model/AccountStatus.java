package com.fintech.ledger.domain.model;

/**
 * Operational state of an account.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    RESTRICTED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
