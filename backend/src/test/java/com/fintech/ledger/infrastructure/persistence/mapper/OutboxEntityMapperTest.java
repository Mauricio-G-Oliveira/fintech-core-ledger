package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.model.OutboxStatus;
import com.fintech.ledger.infrastructure.persistence.entity.OutboxEventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEntityMapperTest {

    private OutboxEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OutboxEntityMapper();
    }

    @Test
    @DisplayName("Should map OutboxEventEntity to domain OutboxEvent correctly")
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEventEntity entity = new OutboxEventEntity(
                id, "TRANSACTION", aggId, "TRANSFER_SETTLED",
                "{\"amount\":100}", OutboxStatus.PENDING, 0, 5,
                now, null, null, now
        );

        OutboxEvent domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(domain.getAggregateId()).isEqualTo(aggId);
        assertThat(domain.getEventType()).isEqualTo("TRANSFER_SETTLED");
        assertThat(domain.getPayload()).isEqualTo("{\"amount\":100}");
        assertThat(domain.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Should map domain OutboxEvent to entity correctly")
    void shouldMapDomainToEntity() {
        UUID aggId = UUID.randomUUID();
        OutboxEvent domain = OutboxEvent.createPending(
                "TRANSACTION", aggId, "TRANSFER_SETTLED", "{\"status\":\"COMMITTED\"}"
        );

        OutboxEventEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getAggregateId()).isEqualTo(aggId);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isEqualTo(0);
    }
}
