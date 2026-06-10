-- =====================================================
-- Tabela przelewów express (jeśli nie istnieje)
-- =====================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(255) NOT NULL PRIMARY KEY,
    sender_account VARCHAR(255) NOT NULL,
    receiver_account VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    sender_bank_id VARCHAR(50),
    receiver_bank_id VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    type VARCHAR(50) DEFAULT 'EXPRESS'
);

-- Indeks dla szybszego wyszukiwania po typie
CREATE INDEX IF NOT EXISTS idx_payments_type ON payments(type);