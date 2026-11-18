package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.repository.OutboxRepositoryPort;
import com.fintech.ledger.infrastructure.persistence.entity.OutboxEventEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.OutboxEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL Secondary Adapter for OutboxRepositoryPort.
 * Implements persistence and batch retrieval of pending outbox events.
 */
@Component
public class PostgreSqlOutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository outboxRepository;
    private final OutboxEntityMapper mapper;

    public PostgreSqlOutboxRepositoryAdapter(SpringDataOutboxRepository outboxRepository,
                                            OutboxEntityMapper mapper) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Optional<OutboxEventEntity> existing = outboxRepository.findById(event.getId());
        OutboxEventEntity entityToSave;

        if (existing.isPresent()) {
            entityToSave = existing.get();
            mapper.updateEntityFromDomain(event, entityToSave);
        } else {
            entityToSave = mapper.toEntity(event);
        }

        OutboxEventEntity saved = outboxRepository.save(entityToSave);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return outboxRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        int sanitizedLimit = Math.max(1, limit);
        return outboxRepository.findPendingEvents(Instant.now(), PageRequest.of(0, sanitizedLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
