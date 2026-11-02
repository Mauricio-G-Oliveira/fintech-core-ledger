package com.fintech.ledger.infrastructure.persistence.entity;

import com.fintech.ledger.domain.model.EntryDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity mapping to table `ledger.ledger_entries`.
 * Append-only immutable journal entry representing an atomic DR or CR leg.
 */
@Entity
@Table(name = "ledger_entries", schema = "ledger")
public class LedgerEntryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private TransactionEntity transaction;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 6, updatable = false)
    private EntryDirection direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "running_balance", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal runningBalance;

    @Column(name = "sequence", nullable = false, updatable = false)
    private int sequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntryEntity() {
    }

    public LedgerEntryEntity(UUID id, TransactionEntity transaction, AccountEntity account,
                             EntryDirection direction, BigDecimal amount, String currency,
                             BigDecimal runningBalance, int sequence, Instant createdAt) {
        this.id = id;
        this.transaction = transaction;
        this.account = account;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.runningBalance = runningBalance;
        this.sequence = sequence;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TransactionEntity getTransaction() { return transaction; }
    public void setTransaction(TransactionEntity transaction) { this.transaction = transaction; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public EntryDirection getDirection() { return direction; }
    public void setDirection(EntryDirection direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
