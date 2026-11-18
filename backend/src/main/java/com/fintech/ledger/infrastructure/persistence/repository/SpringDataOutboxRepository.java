package com.fintech.ledger.infrastructure.persistence.repository;

import com.fintech.ledger.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for OutboxEventEntity.
 * Queries pending outbox records ready for asynchronous dispatching.
 */
@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = 'PENDING' AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(@Param("now") Instant now, Pageable pageable);
}
