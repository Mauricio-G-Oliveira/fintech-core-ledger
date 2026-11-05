package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.AccountDTO;
import com.fintech.ledger.application.usecase.AccountQueryUseCase;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST Controller for querying the Chart of Accounts (COA) and real-time account balances.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Chart of Accounts", description = "Query financial ledger accounts and balances")
public class AccountController {

    private final AccountQueryUseCase accountQueryUseCase;

    public AccountController(AccountQueryUseCase accountQueryUseCase) {
        this.accountQueryUseCase = Objects.requireNonNull(accountQueryUseCase, "accountQueryUseCase must not be null");
    }

    @GetMapping
    @Operation(summary = "List all accounts", description = "Returns all accounts configured in the Chart of Accounts (COA)")
    @ApiResponse(responseCode = "200", description = "List of accounts", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountDTO.class))))
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        return ResponseEntity.ok(accountQueryUseCase.findAllAccounts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieves account metadata and current balance by its unique UUID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountDTO.class))),
        @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountDTO> getAccountById(
            @Parameter(description = "Account UUID", example = "a0000000-0000-0000-0000-000000000003")
            @PathVariable UUID id) {
        return ResponseEntity.ok(accountQueryUseCase.getAccountById(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance", description = "Returns available balance and currency timestamp for an account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account balance retrieved", content = @Content(schema = @Schema(implementation = AccountBalanceDTO.class))),
        @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountBalanceDTO> getAccountBalance(
            @Parameter(description = "Account UUID", example = "a0000000-0000-0000-0000-000000000003")
            @PathVariable UUID id) {
        return ResponseEntity.ok(accountQueryUseCase.getAccountBalance(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get account by code", description = "Retrieves account by its Chart of Accounts code (e.g. 1.1.01.001)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountDTO.class))),
        @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountDTO> getAccountByCode(
            @Parameter(description = "Chart of Accounts code", example = "2.1.02.001")
            @PathVariable String code) {
        return ResponseEntity.ok(accountQueryUseCase.getAccountByCode(code));
    }
}
