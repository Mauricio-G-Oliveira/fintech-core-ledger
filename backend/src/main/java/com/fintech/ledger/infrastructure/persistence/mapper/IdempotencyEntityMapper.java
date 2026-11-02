package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.IdempotencyRecord;
import com.fintech.ledger.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between IdempotencyRecord domain model and IdempotencyRecordEntity JPA entity.
 */
@Component
public class IdempotencyEntityMapper {

    public IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new IdempotencyRecord(
            entity.getIdempotencyKey(),
            entity.getTransactionId(),
            entity.getRequestHash(),
            entity.getResponseBody(),
            entity.getStatusCode(),
            entity.getCreatedAt(),
            entity.getExpiresAt()
        );
    }

    public IdempotencyRecordEntity toEntity(IdempotencyRecord domain) {
        if (domain == null) {
            return null;
        }
        return new IdempotencyRecordEntity(
            domain.idempotencyKey(),
            domain.transactionId(),
            domain.requestHash(),
            domain.responseBody(),
            domain.statusCode(),
            domain.createdAt(),
            domain.expiresAt()
        );
    }
}
