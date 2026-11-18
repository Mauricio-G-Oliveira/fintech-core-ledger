package com.fintech.ledger.domain.repository;

import com.fintech.ledger.domain.model.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port for persisting and querying transactional outbox events.
 */
public interface OutboxRepositoryPort {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> findPendingEvents(int limit);
}
