package com.fintech.ledger.application.service;

import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.TransactionNotFoundException;
import com.fintech.ledger.application.usecase.LedgerQueryUseCase;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Service implementing LedgerQueryUseCase.
 * Provides historical auditability for ledger transactions and journal legs.
 */
@Service
@Transactional(readOnly = true)
public class LedgerQueryService implements LedgerQueryUseCase {

    private final LedgerTransactionRepositoryPort transactionRepositoryPort;

    public LedgerQueryService(LedgerTransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = Objects.requireNonNull(transactionRepositoryPort, "transactionRepositoryPort must not be null");
    }

    @Override
    public List<TransferResult> getRecentTransactions(int limit) {
        int sanitizedLimit = Math.min(Math.max(1, limit), 100);
        return transactionRepositoryPort.findRecent(sanitizedLimit)
                .stream()
                .map(tx -> TransferResult.fromDomain(tx, false))
                .toList();
    }

    @Override
    public TransferResult getTransactionById(UUID id) {
        Objects.requireNonNull(id, "Transaction ID must not be null");
        return transactionRepositoryPort.findById(id)
                .map(tx -> TransferResult.fromDomain(tx, false))
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with ID " + id + " was not found"));
    }

    @Override
    public TransferResult getTransactionByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be null or blank");
        }
        return transactionRepositoryPort.findByIdempotencyKey(idempotencyKey)
                .map(tx -> TransferResult.fromDomain(tx, true))
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with idempotency key '" + idempotencyKey + "' was not found"));
    }
}
