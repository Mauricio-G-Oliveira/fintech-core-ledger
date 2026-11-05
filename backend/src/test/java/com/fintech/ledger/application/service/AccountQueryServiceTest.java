package com.fintech.ledger.application.service;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.AccountDTO;
import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    private AccountQueryService service;

    @BeforeEach
    void setUp() {
        service = new AccountQueryService(accountRepositoryPort);
    }

    @Test
    @DisplayName("Should find all accounts")
    void shouldFindAllAccounts() {
        Account account = Account.create("1.1.01.001", "Reserve", AccountType.ASSET, Money.brl("1000.0000"));
        when(accountRepositoryPort.findAll()).thenReturn(List.of(account));

        List<AccountDTO> results = service.findAllAccounts();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).code()).isEqualTo("1.1.01.001");
    }

    @Test
    @DisplayName("Should get account balance")
    void shouldGetAccountBalance() {
        UUID id = UUID.randomUUID();
        Account account = new Account(
                id, "2.1.02.001", "Wallet", AccountType.LIABILITY,
                Money.brl("450.5000"), AccountStatus.ACTIVE, 1L, null, null
        );
        when(accountRepositoryPort.findById(id)).thenReturn(Optional.of(account));

        AccountBalanceDTO balance = service.getAccountBalance(id);

        assertThat(balance).isNotNull();
        assertThat(balance.availableBalance()).isEqualByComparingTo("450.5000");
        assertThat(balance.currency()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account is missing")
    void shouldThrowWhenAccountMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountById(id))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
