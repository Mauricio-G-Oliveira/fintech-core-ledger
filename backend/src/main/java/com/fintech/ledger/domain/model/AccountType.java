package com.fintech.ledger.domain.model;

/**
 * Standard Chart of Accounts (COA) Account Classifications.
 * Encapsulates financial normal balance rules (Debit Normal vs Credit Normal).
 */
public enum AccountType {
    
    /**
     * Assets (Ativo): Resources owned by the institution (e.g. Central Bank Reserve).
     * Normal Balance: DEBIT (Debits increase, Credits decrease).
     */
    ASSET(true),

    /**
     * Liabilities (Passivo): Obligations owed to third parties (e.g. Customer Wallets, Escrow).
     * Normal Balance: CREDIT (Credits increase, Debits decrease).
     */
    LIABILITY(false),

    /**
     * Equity (Patrimônio Líquido): Net worth / shareholder capital.
     * Normal Balance: CREDIT.
     */
    EQUITY(false),

    /**
     * Revenue (Receitas): Incomes earned (e.g. Interchange fees, spread).
     * Normal Balance: CREDIT.
     */
    REVENUE(false),

    /**
     * Expense (Despesas): Costs incurred in operations.
     * Normal Balance: DEBIT.
     */
    EXPENSE(true);

    private final boolean debitNormal;

    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    public boolean isDebitNormal() {
        return debitNormal;
    }

    public boolean isCreditNormal() {
        return !debitNormal;
    }
}
