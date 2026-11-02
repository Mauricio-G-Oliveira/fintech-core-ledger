package com.fintech.ledger.infrastructure.persistence.repository;

import com.fintech.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for TransactionEntity.
 * Provides optimized fetch queries for transactions and their journal legs.
 */
@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("SELECT t FROM TransactionEntity t LEFT JOIN FETCH t.entries WHERE t.idempotencyKey = :idempotencyKey")
    Optional<TransactionEntity> findByIdempotencyKeyWithEntries(@Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT t FROM TransactionEntity t LEFT JOIN FETCH t.entries WHERE t.id = :id")
    Optional<TransactionEntity> findByIdWithEntries(@Param("id") UUID id);

    @Query("SELECT DISTINCT t FROM TransactionEntity t LEFT JOIN FETCH t.entries ORDER BY t.createdAt DESC")
    List<TransactionEntity> findRecentWithEntries(Pageable pageable);
}
