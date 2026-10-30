package com.fintech.ledger.domain.repository;

import com.fintech.ledger.domain.model.LedgerTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port (Repository Interface) for immutable transaction and journal entries persistence.
 */
public interface LedgerTransactionRepositoryPort {

    Optional<LedgerTransaction> findById(UUID id);

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    LedgerTransaction save(LedgerTransaction transaction);

    List<LedgerTransaction> findRecent(int limit);
}
