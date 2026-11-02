package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import com.fintech.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataAccountRepository;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL Secondary/Driven Adapter for LedgerTransactionRepositoryPort.
 * Persists and reconstitutes immutable transactions and associated journal entries.
 */
@Component
public class PostgreSqlLedgerTransactionRepositoryAdapter implements LedgerTransactionRepositoryPort {

    private final SpringDataTransactionRepository transactionRepository;
    private final SpringDataAccountRepository accountRepository;
    private final TransactionEntityMapper mapper;

    public PostgreSqlLedgerTransactionRepositoryAdapter(SpringDataTransactionRepository transactionRepository,
                                                        SpringDataAccountRepository accountRepository,
                                                        TransactionEntityMapper mapper) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<LedgerTransaction> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return transactionRepository.findByIdWithEntries(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKeyWithEntries(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public LedgerTransaction save(LedgerTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");

        TransactionEntity entity = mapper.toEntity(
            transaction,
            accountId -> accountId != null ? accountRepository.getReferenceById(accountId) : null
        );

        TransactionEntity saved = transactionRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<LedgerTransaction> findRecent(int limit) {
        int sanitizedLimit = Math.max(1, limit);
        return transactionRepository.findRecentWithEntries(PageRequest.of(0, sanitizedLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
