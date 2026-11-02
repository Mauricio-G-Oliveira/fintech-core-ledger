package com.fintech.ledger.infrastructure.persistence.repository;

import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AccountEntity.
 * Implements deterministic anti-deadlock pessimistic write locking (SELECT FOR UPDATE)
 * with a 5000ms timeout.
 */
@Repository
public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT a FROM AccountEntity a WHERE a.id IN :ids ORDER BY a.id ASC")
    List<AccountEntity> findWithPessimisticLockByIdInOrderByIdAsc(@Param("ids") List<UUID> ids);
}
