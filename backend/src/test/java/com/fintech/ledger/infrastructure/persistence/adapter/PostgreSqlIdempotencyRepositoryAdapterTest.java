package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.IdempotencyRecord;
import com.fintech.ledger.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.IdempotencyEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataIdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgreSqlIdempotencyRepositoryAdapterTest {

    @Mock
    private SpringDataIdempotencyRepository idempotencyRepository;

    private IdempotencyEntityMapper mapper;
    private PostgreSqlIdempotencyRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new IdempotencyEntityMapper();
        adapter = new PostgreSqlIdempotencyRepositoryAdapter(idempotencyRepository, mapper);
    }

    @Test
    @DisplayName("Should find idempotency record by key")
    void shouldFindByKey() {
        String key = "test-idem-key";
        Instant now = Instant.now();
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity(
                key, UUID.randomUUID(), "hash123", "{\"status\":\"OK\"}", 200, now, now.plusSeconds(3600)
        );

        when(idempotencyRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(entity));

        Optional<IdempotencyRecord> result = adapter.findByKey(key);

        assertThat(result).isPresent();
        assertThat(result.get().idempotencyKey()).isEqualTo(key);
        assertThat(result.get().statusCode()).isEqualTo(200);
        assertThat(result.get().responseBody()).isEqualTo("{\"status\":\"OK\"}");
    }

    @Test
    @DisplayName("Should save idempotency record correctly")
    void shouldSaveRecord() {
        IdempotencyRecord record = IdempotencyRecord.of(
                "key-xyz", UUID.randomUUID(), "hash-xyz", "{\"message\":\"Created\"}", 201, Instant.now().plusSeconds(86400)
        );

        when(idempotencyRepository.save(any(IdempotencyRecordEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        IdempotencyRecord saved = adapter.save(record);

        assertThat(saved).isNotNull();
        assertThat(saved.idempotencyKey()).isEqualTo("key-xyz");
        verify(idempotencyRepository).save(any(IdempotencyRecordEntity.class));
    }

    @Test
    @DisplayName("Should check existence by key")
    void shouldCheckExistsByKey() {
        when(idempotencyRepository.existsByIdempotencyKey("valid-key")).thenReturn(true);
        when(idempotencyRepository.existsByIdempotencyKey("unknown-key")).thenReturn(false);

        assertThat(adapter.existsByKey("valid-key")).isTrue();
        assertThat(adapter.existsByKey("unknown-key")).isFalse();
    }
}
