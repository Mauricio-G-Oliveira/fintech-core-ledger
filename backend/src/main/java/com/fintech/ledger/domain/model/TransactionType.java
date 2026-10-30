package com.fintech.ledger.domain.model;

/**
 * High-level business classification of a ledger transaction.
 */
public enum TransactionType {
    INSTANT_PAYMENT,
    CLEARING_SETTLEMENT,
    ESCROW_RELEASE,
    FEE_ACCRUAL
}
