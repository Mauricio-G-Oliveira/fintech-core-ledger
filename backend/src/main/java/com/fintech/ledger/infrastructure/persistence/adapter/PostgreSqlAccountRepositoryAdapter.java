package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.AccountEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL Secondary/Driven Adapter for AccountRepositoryPort.
 * Implements deterministic anti-deadlock locking and mapping between JPA entities and Domain Aggregates.
 */
@Component
public class PostgreSqlAccountRepositoryAdapter implements AccountRepositoryPort {

    private final SpringDataAccountRepository accountRepository;
    private final AccountEntityMapper mapper;

    public PostgreSqlAccountRepositoryAdapter(SpringDataAccountRepository accountRepository, 
                                              AccountEntityMapper mapper) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<Account> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return accountRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return accountRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return accountRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Account save(Account account) {
        Objects.requireNonNull(account, "Account must not be null");

        Optional<AccountEntity> existing = accountRepository.findById(account.getId());
        AccountEntity entityToSave;

        if (existing.isPresent()) {
            entityToSave = existing.get();
            mapper.updateEntityFromDomain(account, entityToSave);
        } else {
            entityToSave = mapper.toEntity(account);
        }

        AccountEntity saved = accountRepository.save(entityToSave);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Account> findByIdsForUpdate(List<UUID> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        // Account IDs must be queried in ascending order to prevent deadlocks
        List<UUID> sortedIds = accountIds.stream().sorted().distinct().toList();
        return accountRepository.findWithPessimisticLockByIdInOrderByIdAsc(sortedIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
