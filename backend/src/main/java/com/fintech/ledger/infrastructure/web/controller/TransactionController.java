package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.usecase.LedgerQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST Controller for auditing immutable transactions and double-entry journal legs.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Ledger Audit Journal", description = "Audit immutable financial transactions and journal entry legs")
public class TransactionController {

    private final LedgerQueryUseCase ledgerQueryUseCase;

    public TransactionController(LedgerQueryUseCase ledgerQueryUseCase) {
        this.ledgerQueryUseCase = Objects.requireNonNull(ledgerQueryUseCase, "ledgerQueryUseCase must not be null");
    }

    @GetMapping
    @Operation(summary = "List recent transactions", description = "Returns recent transactions with all double-entry debit/credit legs")
    @ApiResponse(responseCode = "200", description = "Recent transaction journal entries",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransferResult.class))))
    public ResponseEntity<List<TransferResult>> getRecentTransactions(
            @Parameter(description = "Maximum number of transactions to return (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ledgerQueryUseCase.getRecentTransactions(limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Returns transaction header, legs, and Merkle hash verification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = TransferResult.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransferResult> getTransactionById(
            @Parameter(description = "Transaction UUID")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ledgerQueryUseCase.getTransactionById(id));
    }

    @GetMapping("/idempotency/{idempotencyKey}")
    @Operation(summary = "Get transaction by Idempotency Key", description = "Finds previously processed transaction by its client idempotency key")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = TransferResult.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransferResult> getTransactionByIdempotencyKey(
            @Parameter(description = "Client Idempotency Key")
            @PathVariable String idempotencyKey) {
        return ResponseEntity.ok(ledgerQueryUseCase.getTransactionByIdempotencyKey(idempotencyKey));
    }
}
