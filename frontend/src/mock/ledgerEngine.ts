import { Account, LedgerTransaction, SystemMetrics } from '../types/ledger';

// Initial Chart of Accounts (COA) adhering to financial accounting standards
export const INITIAL_ACCOUNTS: Account[] = [
  {
    id: 'acc-1001',
    code: '1.1.01.001',
    name: 'Central Bank / Clearing Reserve',
    type: 'ASSET',
    balance: 5000000.00,
    currency: 'BRL',
    status: 'ACTIVE',
    version: 1,
    updatedAt: new Date(Date.now() - 3600000).toISOString()
  },
  {
    id: 'acc-2001',
    code: '2.1.01.001',
    name: 'TechPay Liquidity Escrow',
    type: 'LIABILITY',
    balance: 1250000.50,
    currency: 'BRL',
    status: 'ACTIVE',
    version: 1,
    updatedAt: new Date(Date.now() - 1800000).toISOString()
  },
  {
    id: 'acc-2002',
    code: '2.1.02.001',
    name: 'Nexus Corp Settlement Account',
    type: 'LIABILITY',
    balance: 450000.00,
    currency: 'BRL',
    status: 'ACTIVE',
    version: 1,
    updatedAt: new Date(Date.now() - 900000).toISOString()
  },
  {
    id: 'acc-2003',
    code: '2.1.02.002',
    name: 'Aura Retail Digital Wallet',
    type: 'LIABILITY',
    balance: 75240.25,
    currency: 'BRL',
    status: 'ACTIVE',
    version: 1,
    updatedAt: new Date(Date.now() - 300000).toISOString()
  },
  {
    id: 'acc-4001',
    code: '4.1.01.001',
    name: 'Engine Interchange & Network Fee Revenue',
    type: 'REVENUE',
    balance: 14820.75,
    currency: 'BRL',
    status: 'ACTIVE',
    version: 1,
    updatedAt: new Date(Date.now() - 60000).toISOString()
  }
];

export const INITIAL_TRANSACTIONS: LedgerTransaction[] = [
  {
    id: 'tx-fin-9981',
    idempotencyKey: 'idem-2026-10-27-001-a1b2',
    type: 'INSTANT_PAYMENT',
    status: 'COMMITTED',
    amount: 15000.00,
    currency: 'BRL',
    description: 'B2B Instant Liquidity Settlement: TechPay -> Nexus Corp',
    createdAt: new Date(Date.now() - 120000).toISOString(),
    settlementLatencyMs: 24,
    hash: '0x8f4d92a017e8b34190c427f9103e5c9b68a4e320f81d4b679a832145bcf6d7e1',
    entries: [
      {
        id: 'entry-9981-1',
        transactionId: 'tx-fin-9981',
        accountId: 'acc-2001',
        accountCode: '2.1.01.001',
        accountName: 'TechPay Liquidity Escrow',
        direction: 'DEBIT',
        amount: 15000.00,
        currency: 'BRL',
        runningBalance: 1250000.50,
        sequence: 1
      },
      {
        id: 'entry-9981-2',
        transactionId: 'tx-fin-9981',
        accountId: 'acc-2002',
        accountCode: '2.1.02.001',
        accountName: 'Nexus Corp Settlement Account',
        direction: 'CREDIT',
        amount: 15000.00,
        currency: 'BRL',
        runningBalance: 450000.00,
        sequence: 2
      }
    ]
  },
  {
    id: 'tx-fin-9980',
    idempotencyKey: 'idem-2026-10-27-002-c3d4',
    type: 'FEE_ACCRUAL',
    status: 'COMMITTED',
    amount: 15.00,
    currency: 'BRL',
    description: 'Instant Engine Clearing Network Fee (0.1%)',
    createdAt: new Date(Date.now() - 119500).toISOString(),
    settlementLatencyMs: 12,
    hash: '0x3a19bc8f4201e7498c55209db89a311cf55798a0c2e39148b598fca20478129e',
    entries: [
      {
        id: 'entry-9980-1',
        transactionId: 'tx-fin-9980',
        accountId: 'acc-2002',
        accountCode: '2.1.02.001',
        accountName: 'Nexus Corp Settlement Account',
        direction: 'DEBIT',
        amount: 15.00,
        currency: 'BRL',
        runningBalance: 449985.00,
        sequence: 1
      },
      {
        id: 'entry-9980-2',
        transactionId: 'tx-fin-9980',
        accountId: 'acc-4001',
        accountCode: '4.1.01.001',
        accountName: 'Engine Interchange & Network Fee Revenue',
        direction: 'CREDIT',
        amount: 15.00,
        currency: 'BRL',
        runningBalance: 14820.75,
        sequence: 2
      }
    ]
  }
];

export function computeMetrics(transactions: LedgerTransaction[]): SystemMetrics {
  let debits = 0;
  let credits = 0;
  let committedCount = 0;
  let totalLatency = 0;

  transactions.forEach(tx => {
    if (tx.status === 'COMMITTED') {
      committedCount++;
      totalLatency += tx.settlementLatencyMs;
      tx.entries.forEach(entry => {
        if (entry.direction === 'DEBIT') debits += entry.amount;
        if (entry.direction === 'CREDIT') credits += entry.amount;
      });
    }
  });

  const isBalanced = Math.abs(debits - credits) < 0.0001;

  return {
    totalDebits: debits,
    totalCredits: credits,
    isBalanced,
    tps: 1840,
    p99LatencyMs: committedCount > 0 ? Math.round(totalLatency / committedCount) : 18,
    idempotencyCacheHits: 142,
    totalTransactions: transactions.length
  };
}

// Pseudo-SHA-256 for cryptographic immutability demonstration in client preview
export function generateHash(input: string): string {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    const char = input.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash |= 0;
  }
  const hex = Math.abs(hash).toString(16).padStart(8, '0');
  const salt = Math.random().toString(16).substring(2, 10);
  const salt2 = Math.random().toString(16).substring(2, 10);
  const salt3 = Math.random().toString(16).substring(2, 10);
  return `0x${hex}${salt}${salt2}${salt3}`.substring(0, 66);
}
