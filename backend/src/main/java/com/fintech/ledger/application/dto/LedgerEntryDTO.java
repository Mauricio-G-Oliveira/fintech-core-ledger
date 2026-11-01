package com.fintech.ledger.application.dto;

import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing an individual Double-Entry bookkeeping leg.
 */
public record LedgerEntryDTO(
    UUID id,
    UUID accountId,
    String accountCode,
    String accountName,
    EntryDirection direction,
    BigDecimal amount,
    BigDecimal runningBalance,
    String currency,
    int sequence,
    Instant createdAt
) {
    public static LedgerEntryDTO fromDomain(LedgerEntry entry) {
        return new LedgerEntryDTO(
            entry.id(),
            entry.accountId(),
            entry.accountCode(),
            entry.accountName(),
            entry.direction(),
            entry.amount().amount(),
            entry.runningBalance().amount(),
            entry.amount().currency().getCurrencyCode(),
            entry.sequence(),
            entry.createdAt()
        );
    }
}
