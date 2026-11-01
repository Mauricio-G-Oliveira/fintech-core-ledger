package com.fintech.ledger.domain.repository;

import com.fintech.ledger.domain.model.IdempotencyRecord;

import java.util.Optional;

/**
 * Output Port (Repository Interface) for persisting and retrieving idempotency records.
 */
public interface IdempotencyRepositoryPort {

    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    IdempotencyRecord save(IdempotencyRecord record);

    boolean existsByKey(String idempotencyKey);
}
