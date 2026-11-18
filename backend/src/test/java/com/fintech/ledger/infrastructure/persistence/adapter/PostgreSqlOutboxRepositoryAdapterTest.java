package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.model.OutboxStatus;
import com.fintech.ledger.infrastructure.persistence.entity.OutboxEventEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.OutboxEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Persistence Adapter Tests: PostgreSqlOutboxRepositoryAdapter")
class PostgreSqlOutboxRepositoryAdapterTest {

    @Mock
    private SpringDataOutboxRepository springDataRepository;

    private OutboxEntityMapper mapper;
    private PostgreSqlOutboxRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new OutboxEntityMapper();
        adapter = new PostgreSqlOutboxRepositoryAdapter(springDataRepository, mapper);
    }

    @Test
    @DisplayName("Should find outbox event by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        OutboxEventEntity entity = new OutboxEventEntity(
                id, "TRANSACTION", aggId, "TRANSFER_SETTLED",
                "{\"amount\":100}", OutboxStatus.PENDING, 0, 5,
                Instant.now(), null, null, Instant.now()
        );

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<OutboxEvent> result = adapter.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getAggregateId()).isEqualTo(aggId);
        assertThat(result.get().getEventType()).isEqualTo("TRANSFER_SETTLED");
        assertThat(result.get().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Should return empty optional when ID is null")
    void shouldReturnEmptyWhenIdIsNull() {
        Optional<OutboxEvent> result = adapter.findById(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find pending events with sanitized limit")
    void shouldFindPendingEvents() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        OutboxEventEntity entity = new OutboxEventEntity(
                id, "TRANSACTION", aggId, "TRANSFER_SETTLED",
                "{\"amount\":50}", OutboxStatus.PENDING, 0, 5,
                Instant.now(), null, null, Instant.now()
        );

        when(springDataRepository.findPendingEvents(any(Instant.class), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(entity));

        List<OutboxEvent> pending = adapter.findPendingEvents(10);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(id);
        verify(springDataRepository).findPendingEvents(any(Instant.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Should save new outbox event")
    void shouldSaveNewOutboxEvent() {
        UUID aggId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.createPending("TRANSACTION", aggId, "TRANSFER_SETTLED", "{\"status\":\"OK\"}");

        when(springDataRepository.findById(event.getId())).thenReturn(Optional.empty());
        when(springDataRepository.save(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent saved = adapter.save(event);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(event.getId());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(springDataRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("Should update existing outbox event when marked as published")
    void shouldUpdateExistingOutboxEvent() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        OutboxEventEntity existingEntity = new OutboxEventEntity(
                id, "TRANSACTION", aggId, "TRANSFER_SETTLED",
                "{\"amount\":100}", OutboxStatus.PENDING, 0, 5,
                Instant.now(), null, null, Instant.now()
        );

        OutboxEvent domain = new OutboxEvent(
                id, "TRANSACTION", aggId, "TRANSFER_SETTLED",
                "{\"amount\":100}", OutboxStatus.PUBLISHED, 0, 5,
                null, Instant.now(), null, Instant.now()
        );

        when(springDataRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent saved = adapter.save(domain);

        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(existingEntity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(existingEntity.getPublishedAt()).isNotNull();
    }
}
