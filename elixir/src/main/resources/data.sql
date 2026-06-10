-- =====================================================
-- Tabela banków (kont rozliczeniowych banków w NBP)
-- =====================================================
CREATE TABLE IF NOT EXISTS bank_accounts (
    bank_id VARCHAR(50) PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    debt_limit NUMERIC(19,2) NOT NULL,
    blocked BOOLEAN NOT NULL,
    overlimit_since TIMESTAMP NULL,
    blocked_at TIMESTAMP NULL
);

-- Seed danych dla bank_accounts
INSERT INTO bank_accounts (bank_id, bank_name, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
    ('NBP',    'Narodowy Bank Polski', 10000000, 0,       false, NULL, NULL),
    ('BANK_A', 'Bank A',                5000000, 2000000, false, NULL, NULL),
    ('BANK_B', 'Bank B',                5000000, 2000000, false, NULL, NULL),
    ('BANK_C', 'Bank C',                5000000, 2000000, false, NULL, NULL)
ON CONFLICT (bank_id) DO UPDATE SET
    bank_name = EXCLUDED.bank_name,
    balance = EXCLUDED.balance,
    debt_limit = EXCLUDED.debt_limit,
    blocked = EXCLUDED.blocked,
    overlimit_since = EXCLUDED.overlimit_since,
    blocked_at = EXCLUDED.blocked_at;

-- =====================================================
-- Tabela z możliwością wielu kont rozliczeniowych na bank
-- =====================================================
CREATE TABLE IF NOT EXISTS settlement_bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    bank_id VARCHAR(50) NOT NULL,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    is_default BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_settlement_bank_accounts_bank
        FOREIGN KEY (bank_id) REFERENCES bank_accounts(bank_id)
);

-- Seed danych dla settlement_bank_accounts
INSERT INTO settlement_bank_accounts (bank_id, account_number, is_default)
VALUES
    ('NBP',    '10100100000000000000000000', true),
    ('BANK_A', '11111100000000000000000001', true),
    ('BANK_A', '11111100000000000000000002', false),
    ('BANK_B', '22222200000000000000000002', true),
    ('BANK_B', '22222200000000000000000003', false),
    ('BANK_C', '33333300000000000000000003', true)
ON CONFLICT (account_number) DO NOTHING;