package com.fintech.ledger.application.usecase;

import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;

/**
 * Primary Input Port for executing instant transfers in the Core Ledger Engine.
 */
public interface InstantTransferUseCase {

    /**
     * Atomically executes a double-entry financial transfer between two accounts with:
     * - Exactly-once semantics via Idempotency Key validation
     * - Deadlock-free pessimistic concurrency control via deterministic account ID sorting
     * - Double-entry balancing: Sum(Debits) == Sum(Credits)
     * 
     * @param command input transfer payload
     * @return TransferResult containing settlement details and ledger legs
     */
    TransferResult transfer(TransferCommand command);

    default TransferResult execute(TransferCommand command) {
        return transfer(command);
    }
}
