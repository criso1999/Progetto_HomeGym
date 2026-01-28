-- 2026-01-27-add-subscriptions.sql

-- Tabella piani di abbonamento
CREATE TABLE IF NOT EXISTS subscription_plan (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE, -- e.g. MONTHLY, SEMIYEAR, ANNUAL
  name VARCHAR(100) NOT NULL,
  description TEXT,
  duration_days INT NOT NULL, -- durata in giorni
  price_cents BIGINT NOT NULL, -- prezzo in cents per evitare float
  currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella sottoscrizioni utente
CREATE TABLE IF NOT EXISTS subscription (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL, -- fk a utenti.id (se vuoi aggiungere FK)
  plan_id INT NOT NULL,
  status VARCHAR(30) NOT NULL, -- PENDING, ACTIVE, CANCELLED, EXPIRED
  price_cents BIGINT NOT NULL,
  currency VARCHAR(10) NOT NULL,
  start_date DATE,
  end_date DATE,
  payment_provider VARCHAR(50), -- es. stripe
  payment_provider_subscription_id VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Optional index
CREATE INDEX idx_subscription_user ON subscription(user_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_subscription_end_date ON subscription(end_date);

-- Seed: tre piani (prezzi di esempio)
INSERT INTO subscription_plan (code, name, description, duration_days, price_cents, currency)
VALUES
('MONTHLY', 'Mensile', 'Abbonamento mensile', 30, 999, 'EUR'),
('SEMIYEAR', 'Semestrale', 'Abbonamento 6 mesi', 183, 4999, 'EUR'),
('ANNUAL', 'Annuale', 'Abbonamento annuale', 365, 8999, 'EUR')
ON DUPLICATE KEY UPDATE active = VALUES(active);
