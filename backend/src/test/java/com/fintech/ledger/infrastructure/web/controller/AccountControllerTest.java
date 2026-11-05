package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.AccountDTO;
import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.application.usecase.AccountQueryUseCase;
import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountQueryUseCase accountQueryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountController controller = new AccountController(accountQueryUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should return 200 OK with list of all accounts")
    void shouldReturnAllAccounts() throws Exception {
        UUID id = UUID.randomUUID();
        AccountDTO acc = new AccountDTO(
                id, "1.1.01.001", "Central Reserve", AccountType.ASSET,
                new BigDecimal("5000000.0000"), "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );

        when(accountQueryUseCase.findAllAccounts()).thenReturn(List.of(acc));

        mockMvc.perform(get("/api/v1/accounts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("1.1.01.001"))
                .andExpect(jsonPath("$[0].balance").value(5000000.0000));
    }

    @Test
    @DisplayName("Should return 200 OK with account details by ID")
    void shouldReturnAccountById() throws Exception {
        UUID id = UUID.randomUUID();
        AccountDTO acc = new AccountDTO(
                id, "2.1.02.001", "Wallet", AccountType.LIABILITY,
                new BigDecimal("1250.0000"), "BRL", AccountStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );

        when(accountQueryUseCase.getAccountById(id)).thenReturn(acc);

        mockMvc.perform(get("/api/v1/accounts/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("2.1.02.001"));
    }

    @Test
    @DisplayName("Should return 200 OK with account balance")
    void shouldReturnAccountBalance() throws Exception {
        UUID id = UUID.randomUUID();
        AccountBalanceDTO balance = new AccountBalanceDTO(
                id, "2.1.02.001", "Wallet", new BigDecimal("750.2500"), "BRL", Instant.now()
        );

        when(accountQueryUseCase.getAccountBalance(id)).thenReturn(balance);

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(750.2500))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when account is not found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountQueryUseCase.getAccountById(id))
                .thenThrow(new AccountNotFoundException("Account with ID " + id + " was not found"));

        mockMvc.perform(get("/api/v1/accounts/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
