package com.fintech.ledger.domain.model;

/**
 * Direction of a ledger entry (Double-Entry Bookkeeping leg).
 */
public enum EntryDirection {
    DEBIT,
    CREDIT;

    public boolean isDebit() {
        return this == DEBIT;
    }

    public boolean isCredit() {
        return this == CREDIT;
    }

    public EntryDirection opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
