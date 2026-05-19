INSERT INTO bank_accounts (bank_id, bank_name, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
  ('NBP',    'Narodowy Bank Polski', 10000000, 0,       false, NULL, NULL),
  ('BANK_A', 'Bank A',               5000000,  2000000, false, NULL, NULL),
  ('BANK_B', 'Bank B',               5000000,  2000000, false, NULL, NULL)
ON CONFLICT (bank_id) DO NOTHING;