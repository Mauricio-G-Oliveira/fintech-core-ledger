package com.fintech.ledger.application.dto;

import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result DTO containing the committed transaction confirmation, double-entry legs,
 * cryptographic hash seal, and whether this was an idempotent replay.
 */
public record TransferResult(
    UUID transactionId,
    String idempotencyKey,
    TransactionType type,
    TransactionStatus status,
    BigDecimal amount,
    String currency,
    String description,
    int latencyMs,
    String hash,
    Instant createdAt,
    boolean isIdempotentReplay,
    List<LedgerEntryDTO> entries
) {
    public static TransferResult fromDomain(LedgerTransaction tx, boolean isReplay) {
        List<LedgerEntryDTO> entryDTOs = tx.getEntries().stream()
            .map(LedgerEntryDTO::fromDomain)
            .toList();

        return new TransferResult(
            tx.getId(),
            tx.getIdempotencyKey(),
            tx.getType(),
            tx.getStatus(),
            tx.getAmount().amount(),
            tx.getAmount().currency().getCurrencyCode(),
            tx.getDescription(),
            tx.getSettlementLatencyMs(),
            tx.getHash(),
            tx.getCreatedAt(),
            isReplay,
            entryDTOs
        );
    }
}
