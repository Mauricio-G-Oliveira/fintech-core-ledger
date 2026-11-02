package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityMapperTest {

    private AccountEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AccountEntityMapper();
    }

    @Test
    @DisplayName("Should map AccountEntity to Domain Account correctly")
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        AccountEntity entity = new AccountEntity(
                id,
                "1.1.01.001",
                "Central Bank Clearing",
                AccountType.ASSET,
                new BigDecimal("500000.0000"),
                "BRL",
                AccountStatus.ACTIVE,
                2L,
                now,
                now
        );

        Account domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getCode()).isEqualTo("1.1.01.001");
        assertThat(domain.getName()).isEqualTo("Central Bank Clearing");
        assertThat(domain.getType()).isEqualTo(AccountType.ASSET);
        assertThat(domain.getBalance()).isEqualTo(Money.brl("500000.0000"));
        assertThat(domain.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(domain.getVersion()).isEqualTo(2L);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should map Domain Account to AccountEntity correctly")
    void shouldMapDomainToEntity() {
        Account domain = Account.create("2.1.02.001", "Customer Wallet", AccountType.LIABILITY, Money.brl("150.5000"));

        AccountEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getCode()).isEqualTo("2.1.02.001");
        assertThat(entity.getName()).isEqualTo("Customer Wallet");
        assertThat(entity.getType()).isEqualTo(AccountType.LIABILITY);
        assertThat(entity.getBalance()).isEqualTo(new BigDecimal("150.5000"));
        assertThat(entity.getCurrency()).isEqualTo("BRL");
        assertThat(entity.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should update existing entity mutable fields from domain")
    void shouldUpdateEntityFromDomain() {
        Account domain = Account.create("2.1.02.001", "Customer Wallet", AccountType.LIABILITY, Money.brl("200.0000"));
        AccountEntity entity = new AccountEntity();
        entity.setBalance(new BigDecimal("100.0000"));
        entity.setStatus(AccountStatus.FROZEN);

        mapper.updateEntityFromDomain(domain, entity);

        assertThat(entity.getBalance()).isEqualTo(new BigDecimal("200.0000"));
        assertThat(entity.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(entity.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
    }
}
