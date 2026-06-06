-- Tabela z możliwością wielu kont na bank
CREATE TABLE IF NOT EXISTS settlement_bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    bank_id VARCHAR(50) NOT NULL,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    is_default BOOLEAN NOT NULL DEFAULT false
);

-- Seed danych
INSERT INTO settlement_bank_accounts (bank_id, account_number, is_default)
VALUES
  ('NBP', '10100100000000000000000000', true),
  ('BANK_A', '11111100000000000000000001', true),
  ('BANK_A', '11111100000000000000000002', false),
  ('BANK_B', '22222200000000000000000002', true),
  ('BANK_B', '22222200000000000000000003', false),
  ('BANK_C', '33333300000000000000000003', true)
ON CONFLICT (account_number) DO NOTHING;