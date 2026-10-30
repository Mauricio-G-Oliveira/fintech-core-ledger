package com.fintech.ledger.domain.model;

import com.fintech.ledger.domain.exception.UnbalancedTransactionException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Aggregate Root representing a financial transaction.
 * 
 * Enforces the strict Double-Entry Bookkeeping invariant:
 * - A transaction must contain at least 2 entries (one debit and one credit).
 * - Total Debits must equal Total Credits (Sum(DR) == Sum(CR)).
 * - Computes a tamper-proof SHA-256 Merkle hash over the transaction header and all entries.
 */
public class LedgerTransaction {

    private final UUID id;
    private final String idempotencyKey;
    private final TransactionType type;
    private final TransactionStatus status;
    private final Money amount;
    private final String description;
    private final Integer settlementLatencyMs;
    private final String hash;
    private final Instant createdAt;
    private final List<LedgerEntry> entries;

    private LedgerTransaction(UUID id, String idempotencyKey, TransactionType type, TransactionStatus status,
                              Money amount, String description, Integer settlementLatencyMs,
                              String hash, Instant createdAt, List<LedgerEntry> entries) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.description = description;
        this.settlementLatencyMs = settlementLatencyMs != null ? settlementLatencyMs : 0;
        this.hash = hash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.entries = entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }

    /**
     * Factory method to construct a COMMITTED transaction.
     * Enforces the Double-Entry Bookkeeping invariant.
     */
    public static LedgerTransaction createCommitted(UUID id, String idempotencyKey, TransactionType type,
                                                    Money amount, String description, 
                                                    List<LedgerEntry> entries, Integer latencyMs) {
        Objects.requireNonNull(id, "Transaction ID must not be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");
        Objects.requireNonNull(type, "Transaction type must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(entries, "Entries list must not be null");

        if (entries.size() < 2) {
            throw new UnbalancedTransactionException(
                "A double-entry transaction must have at least 2 legs (debit and credit). Provided: " + entries.size()
            );
        }

        // Validate Double-Entry Invariant: Sum(Debits) == Sum(Credits)
        Money totalDebits = Money.zero(amount.currency());
        Money totalCredits = Money.zero(amount.currency());

        for (LedgerEntry entry : entries) {
            if (entry.direction().isDebit()) {
                totalDebits = totalDebits.add(entry.amount());
            } else {
                totalCredits = totalCredits.add(entry.amount());
            }
        }

        if (!totalDebits.equals(totalCredits)) {
            throw new UnbalancedTransactionException(
                String.format("Double-entry bookkeeping invariant violated! Total Debits [%s] != Total Credits [%s]. Variance = [%s]",
                    totalDebits, totalCredits, totalDebits.subtract(totalCredits))
            );
        }

        if (!amount.equals(totalDebits)) {
            throw new UnbalancedTransactionException(
                String.format("Transaction header amount [%s] does not match total debit leg sum [%s]", amount, totalDebits)
            );
        }

        Instant now = Instant.now();
        String cryptographicHash = calculateHash(id, idempotencyKey, amount, entries, now);

        return new LedgerTransaction(
            id,
            idempotencyKey,
            type,
            TransactionStatus.COMMITTED,
            amount,
            description,
            latencyMs,
            cryptographicHash,
            now,
            entries
        );
    }

    /**
     * Factory method for recording rejected transactions (e.g. idempotency conflicts or insufficient funds).
     */
    public static LedgerTransaction createRejected(UUID id, String idempotencyKey, TransactionType type,
                                                   TransactionStatus status, Money amount, String reason, Integer latencyMs) {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Instant now = Instant.now();

        return new LedgerTransaction(
            id,
            idempotencyKey,
            type,
            status,
            amount,
            reason,
            latencyMs,
            "0x" + "0".repeat(64),
            now,
            Collections.emptyList()
        );
    }

    private static String calculateHash(UUID id, String idempotencyKey, Money amount, 
                                        List<LedgerEntry> entries, Instant createdAt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(id).append("|")
              .append(idempotencyKey).append("|")
              .append(amount.amount()).append("|")
              .append(amount.currency().getCurrencyCode()).append("|")
              .append(createdAt.toEpochMilli());

            for (LedgerEntry entry : entries) {
                sb.append("#")
                  .append(entry.sequence()).append(":")
                  .append(entry.accountId()).append(":")
                  .append(entry.direction()).append(":")
                  .append(entry.amount().amount());
            }

            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return "0x" + HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public Integer getSettlementLatencyMs() { return settlementLatencyMs; }
    public String getHash() { return hash; }
    public Instant getCreatedAt() { return createdAt; }
    public List<LedgerEntry> getEntries() { return entries; }
}
