INSERT INTO bank_accounts (bank_id, bank_name, account_number, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
  ('NBP',    'Narodowy Bank Polski',  '10100100000000000000000000', 10000000, 0,       false, NULL, NULL),
  ('BANK_A', 'Bank A',                '11111100000000000000000001', 5000000,  2000000, false, NULL, NULL),
  ('BANK_B', 'Bank B',                '22222200000000000000000002', 5000000,  2000000, false, NULL, NULL)
ON CONFLICT (bank_id) DO UPDATE SET
  bank_name = EXCLUDED.bank_name,
  account_number = EXCLUDED.account_number,
  balance = EXCLUDED.balance,
  debt_limit = EXCLUDED.debt_limit,
  blocked = EXCLUDED.blocked,
  overlimit_since = EXCLUDED.overlimit_since,
  blocked_at = EXCLUDED.blocked_at;