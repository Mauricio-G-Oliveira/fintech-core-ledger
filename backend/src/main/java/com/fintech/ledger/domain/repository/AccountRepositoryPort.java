package com.fintech.ledger.domain.repository;

import com.fintech.ledger.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port (Repository Interface) for Account persistence and lock management.
 * In Clean Architecture, the domain defines this interface, and infrastructure implements it.
 */
public interface AccountRepositoryPort {

    Optional<Account> findById(UUID id);

    Optional<Account> findByCode(String code);

    List<Account> findAll();

    Account save(Account account);

    /**
     * Finds and locks accounts using pessimistic write lock (SELECT FOR UPDATE)
     * ordered deterministically by UUID ascending to prevent database deadlocks.
     */
    List<Account> findByIdsForUpdate(List<UUID> accountIds);
}
