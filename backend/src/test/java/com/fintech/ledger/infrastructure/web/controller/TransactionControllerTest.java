package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.TransactionNotFoundException;
import com.fintech.ledger.application.usecase.LedgerQueryUseCase;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;
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
class TransactionControllerTest {

    @Mock
    private LedgerQueryUseCase ledgerQueryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(ledgerQueryUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should return list of recent transactions")
    void shouldReturnRecentTransactions() throws Exception {
        UUID txId = UUID.randomUUID();
        TransferResult tx = new TransferResult(
                txId, "key-1", TransactionType.INSTANT_PAYMENT, TransactionStatus.COMMITTED,
                new BigDecimal("50.0000"), "BRL", "Payment", 4, "0xhash", Instant.now(), false, List.of()
        );

        when(ledgerQueryUseCase.getRecentTransactions(10)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/transactions?limit=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value(txId.toString()));
    }

    @Test
    @DisplayName("Should return transaction by ID")
    void shouldReturnTransactionById() throws Exception {
        UUID txId = UUID.randomUUID();
        TransferResult tx = new TransferResult(
                txId, "key-find", TransactionType.INSTANT_PAYMENT, TransactionStatus.COMMITTED,
                new BigDecimal("120.0000"), "BRL", "Payment", 3, "0x123", Instant.now(), false, List.of()
        );

        when(ledgerQueryUseCase.getTransactionById(txId)).thenReturn(tx);

        mockMvc.perform(get("/api/v1/transactions/{id}", txId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("key-find"));
    }

    @Test
    @DisplayName("Should return 404 when transaction is not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID txId = UUID.randomUUID();
        when(ledgerQueryUseCase.getTransactionById(txId))
                .thenThrow(new TransactionNotFoundException("Transaction with ID " + txId + " was not found"));

        mockMvc.perform(get("/api/v1/transactions/{id}", txId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
