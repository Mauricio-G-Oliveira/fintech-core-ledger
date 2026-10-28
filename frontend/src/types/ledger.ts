export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';

export interface Account {
  id: string;
  code: string;
  name: string;
  type: AccountType;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'RESTRICTED';
  version: number; // Optimistic locking
  updatedAt: string;
}

export type EntryDirection = 'DEBIT' | 'CREDIT';

export interface LedgerEntry {
  id: string;
  transactionId: string;
  accountId: string;
  accountCode: string;
  accountName: string;
  direction: EntryDirection;
  amount: number;
  currency: string;
  runningBalance: number;
  sequence: number;
}

export type TransactionStatus = 'COMMITTED' | 'REJECTED_IDEMPOTENT' | 'REJECTED_INSUFFICIENT_FUNDS' | 'REJECTED_LOCK_TIMEOUT';

export interface LedgerTransaction {
  id: string;
  idempotencyKey: string;
  type: 'INSTANT_PAYMENT' | 'CLEARING_SETTLEMENT' | 'ESCROW_RELEASE' | 'FEE_ACCRUAL';
  status: TransactionStatus;
  amount: number;
  currency: string;
  description: string;
  createdAt: string;
  settlementLatencyMs: number;
  hash: string; // Cryptographic Merkle / SHA-256 seal
  entries: LedgerEntry[];
}

export interface InstantTransferPayload {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: string;
  idempotencyKey: string;
  description: string;
  concurrencyLockMode: 'PESSIMISTIC_WRITE' | 'OPTIMISTIC';
}

export interface SystemMetrics {
  totalDebits: number;
  totalCredits: number;
  isBalanced: boolean;
  tps: number;
  p99LatencyMs: number;
  idempotencyCacheHits: number;
  totalTransactions: number;
}
