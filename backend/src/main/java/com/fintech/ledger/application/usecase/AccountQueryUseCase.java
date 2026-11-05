package com.fintech.ledger.application.usecase;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.AccountDTO;

import java.util.List;
import java.util.UUID;

/**
 * Use Case contract for querying the Chart of Accounts and account balances.
 */
public interface AccountQueryUseCase {

    List<AccountDTO> findAllAccounts();

    AccountDTO getAccountById(UUID id);

    AccountBalanceDTO getAccountBalance(UUID id);

    AccountDTO getAccountByCode(String code);
}
