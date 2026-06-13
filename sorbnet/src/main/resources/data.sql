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

INSERT INTO bank_accounts (service_code, bank_id, bank_name, account_number, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
    ('SORBNET', 'BANK_A', 'Bank A - Sorbnet', 'SORBNET-A-00000000000000000001', 10000000.00, 30000000.00, false, NULL, NULL),
    ('SORBNET', 'BANK_B', 'Bank B - Sorbnet', 'SORBNET-B-00000000000000000002', 10000000.00, 30000000.00, false, NULL, NULL),
    ('SORBNET', 'BANK_C', 'Bank C - Sorbnet', 'SORBNET-C-00000000000000000003', 10000000.00, 30000000.00, false, NULL, NULL)
ON CONFLICT (service_code, bank_id) DO UPDATE SET
    bank_name      = EXCLUDED.bank_name,
    account_number = EXCLUDED.account_number,
    balance        = EXCLUDED.balance,
    debt_limit     = EXCLUDED.debt_limit,
    blocked        = EXCLUDED.blocked,
    overlimit_since = EXCLUDED.overlimit_since,
    blocked_at     = EXCLUDED.blocked_at;