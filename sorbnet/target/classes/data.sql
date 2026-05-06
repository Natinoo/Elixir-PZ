INSERT INTO bank_accounts (bank_id, bank_name, balance, debt_limit, blocked, overlimit_since, blocked_at)
VALUES
  ('NBP',   'Narodowy Bank Polski',  10000000, 0,       false, NULL, NULL),
  ('PKO',   'PKO Bank Polski',        5000000, 2000000, false, NULL, NULL),
  ('PEKAO', 'Bank Pekao',             4000000, 1500000, false, NULL, NULL),
  ('MBANK', 'mBank',                  3000000, 1000000, false, NULL, NULL),
  ('ING',   'ING Bank Śląski',        3500000, 1200000, false, NULL, NULL),
  ('BNP',   'BNP Paribas',            2500000, 800000,  false, NULL, NULL)
ON CONFLICT (bank_id) DO NOTHING;