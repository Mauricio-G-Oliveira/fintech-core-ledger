package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between OutboxEvent domain model and OutboxEventEntity JPA entity.
 */
@Component
public class OutboxEntityMapper {

    public OutboxEvent toDomain(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OutboxEvent(
            entity.getId(),
            entity.getAggregateType(),
            entity.getAggregateId(),
            entity.getEventType(),
            entity.getPayload(),
            entity.getStatus(),
            entity.getRetryCount(),
            entity.getMaxRetries(),
            entity.getNextRetryAt(),
            entity.getPublishedAt(),
            entity.getLastError(),
            entity.getCreatedAt()
        );
    }

    public OutboxEventEntity toEntity(OutboxEvent domain) {
        if (domain == null) {
            return null;
        }
        return new OutboxEventEntity(
            domain.getId(),
            domain.getAggregateType(),
            domain.getAggregateId(),
            domain.getEventType(),
            domain.getPayload(),
            domain.getStatus(),
            domain.getRetryCount(),
            domain.getMaxRetries(),
            domain.getNextRetryAt(),
            domain.getPublishedAt(),
            domain.getLastError(),
            domain.getCreatedAt()
        );
    }

    public void updateEntityFromDomain(OutboxEvent domain, OutboxEventEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setStatus(domain.getStatus());
        entity.setRetryCount(domain.getRetryCount());
        entity.setNextRetryAt(domain.getNextRetryAt());
        entity.setPublishedAt(domain.getPublishedAt());
        entity.setLastError(domain.getLastError());
    }
}
