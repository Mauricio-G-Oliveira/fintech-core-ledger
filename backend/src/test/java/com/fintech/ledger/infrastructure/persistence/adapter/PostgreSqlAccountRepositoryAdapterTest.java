package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.AccountEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgreSqlAccountRepositoryAdapterTest {

    @Mock
    private SpringDataAccountRepository accountRepository;

    private AccountEntityMapper mapper;
    private PostgreSqlAccountRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<List<UUID>> sortedIdsCaptor;

    @BeforeEach
    void setUp() {
        mapper = new AccountEntityMapper();
        adapter = new PostgreSqlAccountRepositoryAdapter(accountRepository, mapper);
    }

    @Test
    @DisplayName("Should find account by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        AccountEntity entity = new AccountEntity(id, "1.1.01.001", "Asset", AccountType.ASSET,
                new BigDecimal("100.0000"), "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());

        when(accountRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Account> result = adapter.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should find account by Code")
    void shouldFindByCode() {
        AccountEntity entity = new AccountEntity(UUID.randomUUID(), "1.1.01.001", "Asset", AccountType.ASSET,
                new BigDecimal("100.0000"), "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());

        when(accountRepository.findByCode("1.1.01.001")).thenReturn(Optional.of(entity));

        Optional<Account> result = adapter.findByCode("1.1.01.001");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("1.1.01.001");
    }

    @Test
    @DisplayName("Should enforce deterministic ascending order for SELECT FOR UPDATE to prevent deadlocks")
    void shouldSortIdsAscendingForLocking() {
        UUID idZ = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idM = UUID.fromString("88888888-8888-8888-8888-888888888888");

        List<UUID> unsortedInput = List.of(idZ, idA, idM);

        AccountEntity entityA = new AccountEntity(idA, "A", "Account A", AccountType.LIABILITY,
                BigDecimal.ZERO, "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());
        AccountEntity entityM = new AccountEntity(idM, "M", "Account M", AccountType.LIABILITY,
                BigDecimal.ZERO, "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());
        AccountEntity entityZ = new AccountEntity(idZ, "Z", "Account Z", AccountType.LIABILITY,
                BigDecimal.ZERO, "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());

        when(accountRepository.findWithPessimisticLockByIdInOrderByIdAsc(any()))
                .thenReturn(List.of(entityA, entityM, entityZ));

        List<Account> accounts = adapter.findByIdsForUpdate(unsortedInput);

        assertThat(accounts).hasSize(3);

        verify(accountRepository).findWithPessimisticLockByIdInOrderByIdAsc(sortedIdsCaptor.capture());
        List<UUID> lockedIds = sortedIdsCaptor.getValue();

        assertThat(lockedIds).containsExactly(idA, idM, idZ);
    }

    @Test
    @DisplayName("Should save and update existing account correctly")
    void shouldSaveExistingAccount() {
        UUID id = UUID.randomUUID();
        Account domain = new Account(id, "2.1.02.001", "Wallet", AccountType.LIABILITY,
                Money.brl("350.0000"), AccountStatus.ACTIVE, 2L, Instant.now(), Instant.now());

        AccountEntity existingEntity = new AccountEntity(id, "2.1.02.001", "Wallet", AccountType.LIABILITY,
                new BigDecimal("100.0000"), "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now());

        when(accountRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account saved = adapter.save(domain);

        assertThat(saved.getBalance()).isEqualTo(Money.brl("350.0000"));
        verify(accountRepository).save(existingEntity);
    }
}
