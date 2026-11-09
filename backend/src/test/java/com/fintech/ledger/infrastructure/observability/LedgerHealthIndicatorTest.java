package com.fintech.ledger.infrastructure.observability;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerHealthIndicatorTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @InjectMocks
    private LedgerHealthIndicator healthIndicator;

    @Test
    @DisplayName("Should report UP status when database and chart of accounts are reachable")
    void shouldReportUpWhenOperational() {
        Account acc = Account.create("1.1.01.001", "Central Reserve", AccountType.ASSET, Money.brl("1000.0000"));
        when(accountRepositoryPort.findAll()).thenReturn(List.of(acc));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("ledgerStatus", "OPERATIONAL");
        assertThat(health.getDetails()).containsEntry("totalAccountsConfigured", 1);
        assertThat(health.getDetails()).containsEntry("doubleEntryEngine", "ACTIVE");
    }

    @Test
    @DisplayName("Should report DOWN status when repository fails")
    void shouldReportDownWhenDatabaseFails() {
        when(accountRepositoryPort.findAll()).thenThrow(new RuntimeException("Connection refused"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("ledgerStatus", "DEGRADED");
        assertThat(health.getDetails()).containsEntry("error", "Connection refused");
    }
}
