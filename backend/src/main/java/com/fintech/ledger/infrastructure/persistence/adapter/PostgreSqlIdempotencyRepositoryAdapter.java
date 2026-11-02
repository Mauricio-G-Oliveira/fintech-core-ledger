package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.IdempotencyRecord;
import com.fintech.ledger.domain.repository.IdempotencyRepositoryPort;
import com.fintech.ledger.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.IdempotencyEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataIdempotencyRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * PostgreSQL Secondary/Driven Adapter for IdempotencyRepositoryPort.
 * Provides durable storage for idempotency keys, response caching, and TTL expiry.
 */
@Component
public class PostgreSqlIdempotencyRepositoryAdapter implements IdempotencyRepositoryPort {

    private final SpringDataIdempotencyRepository idempotencyRepository;
    private final IdempotencyEntityMapper mapper;

    public PostgreSqlIdempotencyRepositoryAdapter(SpringDataIdempotencyRepository idempotencyRepository,
                                                 IdempotencyEntityMapper mapper) {
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository, "idempotencyRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        IdempotencyRecordEntity entity = mapper.toEntity(record);
        IdempotencyRecordEntity saved = idempotencyRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return idempotencyRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
