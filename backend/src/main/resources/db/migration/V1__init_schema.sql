-- ==============================================================================
-- V1__init_schema.sql
-- Fintech Core Ledger & Instant Payment Engine
-- Immutability, Double-Entry Invariants, Idempotency and High-Concurrency Schema
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS ledger;

-- 1. Chart of Accounts (COA)
CREATE TABLE ledger.accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'RESTRICTED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_code ON ledger.accounts(code);
CREATE INDEX idx_accounts_type ON ledger.accounts(type);

-- 2. Ledger Transactions (Header)
CREATE TABLE ledger.transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL CHECK (type IN ('INSTANT_PAYMENT', 'CLEARING_SETTLEMENT', 'ESCROW_RELEASE', 'FEE_ACCRUAL')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('COMMITTED', 'REJECTED_IDEMPOTENT', 'REJECTED_INSUFFICIENT_FUNDS', 'REJECTED_LOCK_TIMEOUT')),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    description TEXT,
    settlement_latency_ms INT NOT NULL DEFAULT 0,
    hash VARCHAR(66) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tx_idempotency_key ON ledger.transactions(idempotency_key);
CREATE INDEX idx_tx_created_at ON ledger.transactions(created_at DESC);

-- 3. Double-Entry Bookkeeping Journal Entries (Append-Only Legs)
CREATE TABLE ledger.ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES ledger.transactions(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES ledger.accounts(id) ON DELETE RESTRICT,
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    running_balance NUMERIC(19, 4) NOT NULL,
    sequence INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tx_sequence UNIQUE (transaction_id, sequence)
);

CREATE INDEX idx_entries_tx_id ON ledger.ledger_entries(transaction_id);
CREATE INDEX idx_entries_account_created ON ledger.ledger_entries(account_id, created_at DESC);

-- 4. Idempotency Cache / In-flight Lock Storage
CREATE TABLE ledger.idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    transaction_id UUID REFERENCES ledger.transactions(id),
    request_hash VARCHAR(64) NOT NULL,
    response_body JSONB,
    status_code INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_expires ON ledger.idempotency_records(expires_at);

-- Initial seed data for Chart of Accounts
INSERT INTO ledger.accounts (id, code, name, type, balance, currency, status, version)
VALUES 
    ('a0000000-0000-0000-0000-000000000001', '1.1.01.001', 'Central Bank / Clearing Reserve', 'ASSET', 5000000.0000, 'BRL', 'ACTIVE', 1),
    ('a0000000-0000-0000-0000-000000000002', '2.1.01.001', 'TechPay Liquidity Escrow', 'LIABILITY', 1250000.5000, 'BRL', 'ACTIVE', 1),
    ('a0000000-0000-0000-0000-000000000003', '2.1.02.001', 'Nexus Corp Settlement Account', 'LIABILITY', 450000.0000, 'BRL', 'ACTIVE', 1),
    ('a0000000-0000-0000-0000-000000000004', '2.1.02.002', 'Aura Retail Digital Wallet', 'LIABILITY', 75240.2500, 'BRL', 'ACTIVE', 1),
    ('a0000000-0000-0000-0000-000000000005', '4.1.01.001', 'Engine Interchange Fee Revenue', 'REVENUE', 14820.7500, 'BRL', 'ACTIVE', 1);
