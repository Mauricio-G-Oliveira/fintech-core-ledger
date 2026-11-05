package com.fintech.ledger.application.usecase;

import com.fintech.ledger.application.dto.TransferResult;

import java.util.List;
import java.util.UUID;

/**
 * Use Case contract for auditing transactions and journal entries in the double-entry ledger.
 */
public interface LedgerQueryUseCase {

    List<TransferResult> getRecentTransactions(int limit);

    TransferResult getTransactionById(UUID id);

    TransferResult getTransactionByIdempotencyKey(String idempotencyKey);
}
