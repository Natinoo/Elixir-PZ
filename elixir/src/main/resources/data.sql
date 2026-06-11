DROP TABLE IF EXISTS settlement_bank_accounts;
DROP TABLE IF EXISTS bank_accounts;

CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    service_code VARCHAR(32) NOT NULL,
    bank_id VARCHAR(50) NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    debt_limit NUMERIC(19,2) NOT NULL,
    blocked BOOLEAN NOT NULL,
    overlimit_since TIMESTAMP NULL,
    blocked_at TIMESTAMP NULL,
    CONSTRAINT uk_bank_account_service_bank UNIQUE (service_code, bank_id),
    CONSTRAINT uk_bank_account_number UNIQUE (account_number)
);

CREATE TABLE IF NOT EXISTS settlement_bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    service_code VARCHAR(32) NOT NULL,
    bank_id VARCHAR(50) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uk_settlement_account_service_bank_default UNIQUE (service_code, bank_id, is_default),
    CONSTRAINT uk_settlement_account_number UNIQUE (account_number)
);

-- Jedno konto banku A/B/C w ELIXIR.
INSERT INTO bank_accounts (service_code, bank_id, bank_name, account_number, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
    ('ELIXIR',  'BANK_A', 'Bank A - Elixir',  'ELIXIR-A-00000000000000000001', 5000000.00, 2000000.00, false, NULL, NULL),
    ('ELIXIR',  'BANK_B', 'Bank B - Elixir',  'ELIXIR-B-00000000000000000002', 5000000.00, 2000000.00, false, NULL, NULL),
    ('ELIXIR',  'BANK_C', 'Bank C - Elixir',  'ELIXIR-C-00000000000000000003', 5000000.00, 2000000.00, false, NULL, NULL),

    -- Jedno konto banku A/B/C w SORBNET. Stąd bank może zasilić konto ELIXIR po kliknięciu w GUI SORBNET.
    ('SORBNET', 'BANK_A', 'Bank A - Sorbnet', 'SORBNET-A-00000000000000000001', 10000000.00, 0.00, false, NULL, NULL),
    ('SORBNET', 'BANK_B', 'Bank B - Sorbnet', 'SORBNET-B-00000000000000000002', 10000000.00, 0.00, false, NULL, NULL),
    ('SORBNET', 'BANK_C', 'Bank C - Sorbnet', 'SORBNET-C-00000000000000000003', 10000000.00, 0.00, false, NULL, NULL)
ON CONFLICT (service_code, bank_id) DO UPDATE SET
    bank_name = EXCLUDED.bank_name,
    account_number = EXCLUDED.account_number,
    balance = EXCLUDED.balance,
    debt_limit = EXCLUDED.debt_limit,
    blocked = EXCLUDED.blocked,
    overlimit_since = EXCLUDED.overlimit_since,
    blocked_at = EXCLUDED.blocked_at;

INSERT INTO settlement_bank_accounts (service_code, bank_id, account_number, is_default)
VALUES
    ('ELIXIR',  'BANK_A', 'ELIXIR-A-00000000000000000001', true),
    ('ELIXIR',  'BANK_B', 'ELIXIR-B-00000000000000000002', true),
    ('ELIXIR',  'BANK_C', 'ELIXIR-C-00000000000000000003', true),
    ('SORBNET', 'BANK_A', 'SORBNET-A-00000000000000000001', true),
    ('SORBNET', 'BANK_B', 'SORBNET-B-00000000000000000002', true),
    ('SORBNET', 'BANK_C', 'SORBNET-C-00000000000000000003', true)
ON CONFLICT (account_number) DO NOTHING;