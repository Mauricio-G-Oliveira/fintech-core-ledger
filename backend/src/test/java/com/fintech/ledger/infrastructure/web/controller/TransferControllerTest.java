package com.fintech.ledger.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ledger.application.dto.LedgerEntryDTO;
import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.IdempotencyConflictException;
import com.fintech.ledger.application.usecase.InstantTransferUseCase;
import com.fintech.ledger.domain.exception.InsufficientFundsException;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;
import com.fintech.ledger.infrastructure.web.dto.request.InitiateTransferRequest;
import com.fintech.ledger.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private InstantTransferUseCase instantTransferUseCase;

    @Captor
    private ArgumentCaptor<TransferCommand> commandCaptor;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TransferController controller = new TransferController(instantTransferUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should execute transfer and return 201 Created with cryptographic headers")
    void shouldExecuteTransferSuccessfully() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String idempotencyKey = "client-key-12345";

        InitiateTransferRequest request = new InitiateTransferRequest(
                sourceId, destId, new BigDecimal("100.50"), "BRL", "Test transfer"
        );

        LedgerEntryDTO debitEntry = new LedgerEntryDTO(
                UUID.randomUUID(), sourceId, "2.1.02.001", "Sender",
                EntryDirection.DEBIT, new BigDecimal("100.5000"), new BigDecimal("899.5000"), "BRL", 1, Instant.now()
        );
        LedgerEntryDTO creditEntry = new LedgerEntryDTO(
                UUID.randomUUID(), destId, "2.1.02.002", "Receiver",
                EntryDirection.CREDIT, new BigDecimal("100.5000"), new BigDecimal("1100.5000"), "BRL", 2, Instant.now()
        );

        TransferResult result = new TransferResult(
                txId, idempotencyKey, TransactionType.INSTANT_PAYMENT, TransactionStatus.COMMITTED,
                new BigDecimal("100.5000"), "BRL", "Test transfer", 7, "0xfe98ba123",
                Instant.now(), false, List.of(debitEntry, creditEntry)
        );

        when(instantTransferUseCase.execute(any(TransferCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Key", idempotencyKey))
                .andExpect(header().string("Idempotency-Replay", "false"))
                .andExpect(header().string("X-Cryptographic-Hash", "0xfe98ba123"))
                .andExpect(header().string("X-Settlement-Latency-Ms", "7"))
                .andExpect(header().string("Location", "/api/v1/transactions/" + txId))
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.amount").value(100.5000))
                .andExpect(jsonPath("$.entries.length()").value(2));

        verify(instantTransferUseCase).execute(commandCaptor.capture());
        TransferCommand captured = commandCaptor.getValue();
        assertThat(captured.sourceAccountId()).isEqualTo(sourceId);
        assertThat(captured.destinationAccountId()).isEqualTo(destId);
        assertThat(captured.idempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("Should return 200 OK when request is an idempotent replay")
    void shouldReturnOkForIdempotentReplay() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String idempotencyKey = "replayed-key";

        InitiateTransferRequest request = new InitiateTransferRequest(
                sourceId, destId, new BigDecimal("50.00"), "BRL", "Duplicate memo"
        );

        TransferResult replayResult = new TransferResult(
                txId, idempotencyKey, TransactionType.INSTANT_PAYMENT, TransactionStatus.COMMITTED,
                new BigDecimal("50.0000"), "BRL", "Duplicate memo", 2, "0x123abc",
                Instant.now(), true, List.of()
        );

        when(instantTransferUseCase.execute(any(TransferCommand.class))).thenReturn(replayResult);

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replay", "true"))
                .andExpect(jsonPath("$.isIdempotentReplay").value(true));
    }

    @Test
    @DisplayName("Should return 422 Unprocessable Entity on InsufficientFundsException")
    void shouldReturn422OnInsufficientFunds() throws Exception {
        InitiateTransferRequest request = new InitiateTransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("999999.00"), "BRL", "Overdraft"
        );

        when(instantTransferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new InsufficientFundsException("Account 2.1.02.001 has insufficient funds"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    @DisplayName("Should return 409 Conflict on IdempotencyConflictException")
    void shouldReturn409OnIdempotencyConflict() throws Exception {
        InitiateTransferRequest request = new InitiateTransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), "BRL", "Payload conflict"
        );

        when(instantTransferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new IdempotencyConflictException("Payload hash conflict for key 'key-1'"));

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency Key Conflict"))
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("Should auto-generate idempotency key when header is omitted")
    void shouldAutoGenerateIdempotencyKeyWhenHeaderMissing() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        InitiateTransferRequest request = new InitiateTransferRequest(
                sourceId, destId, new BigDecimal("25.00"), "BRL", "No header"
        );

        TransferResult result = new TransferResult(
                UUID.randomUUID(), "auto-key", TransactionType.INSTANT_PAYMENT, TransactionStatus.COMMITTED,
                new BigDecimal("25.0000"), "BRL", "No header", 5, "0xhash",
                Instant.now(), false, List.of()
        );

        when(instantTransferUseCase.execute(commandCaptor.capture())).thenReturn(result);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        TransferCommand captured = commandCaptor.getValue();
        assertThat(captured.idempotencyKey()).isNotBlank();
        assertThat(UUID.fromString(captured.idempotencyKey())).isNotNull(); // asserts it's valid UUID format
    }
}
