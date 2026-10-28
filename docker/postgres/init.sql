-- PostgreSQL Database Initial Setup for Fintech Core Ledger
-- Extensions for high-concurrency UUID generation and cryptographic auditing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set timezone to UTC for financial accuracy
SET timezone TO 'UTC';

-- Schema creation
CREATE SCHEMA IF NOT EXISTS ledger;
COMMENT ON SCHEMA ledger IS 'Core Double-Entry Bookkeeping & Settlement Schema';
