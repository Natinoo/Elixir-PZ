CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(255) NOT NULL PRIMARY KEY,
    sender_name VARCHAR(255),
    receiver_name VARCHAR(255),
    sender_account VARCHAR(255) NOT NULL,
    receiver_account VARCHAR(255) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    sender_bank_id VARCHAR(50) NOT NULL,
    receiver_bank_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    type VARCHAR(50) DEFAULT 'EXPRESS',
    held_reason VARCHAR(255),
    processed_at TIMESTAMP
);

ALTER TABLE payments ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS sender_bank_id VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receiver_bank_id VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS held_reason VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;
ALTER TABLE payments ALTER COLUMN amount TYPE NUMERIC(19,2) USING amount::numeric;

CREATE INDEX IF NOT EXISTS idx_payments_type ON payments(type);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_sender_bank_status ON payments(sender_bank_id, status);

CREATE TABLE IF NOT EXISTS bank_accounts (
    bank_id VARCHAR(50) NOT NULL PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    debt_limit NUMERIC(19,2) NOT NULL,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    overlimit_since TIMESTAMP,
    blocked_at TIMESTAMP
);

INSERT INTO bank_accounts (bank_id, bank_name, balance, debt_limit, blocked, overlimit_since, blocked_at) VALUES
('BANK_A', 'Bank A Express', 5000000.00, 1000.00, false, null, null),
('BANK_B', 'Bank B Express', 5000000.00, 1000.00, false, null, null),
('BANK_C', 'Bank C Express', 5000000.00, 1000.00, false, null, null)
ON CONFLICT (bank_id) DO NOTHING;