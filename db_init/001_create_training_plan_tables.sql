-- 001_create_training_plan_tables.sql

-- Creazione delle tabelle per la gestione dei piani di allenamento -->
CREATE TABLE IF NOT EXISTS training_plan (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  content TEXT,
  created_by INT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  CONSTRAINT fk_tp_created_by 
    FOREIGN KEY (created_by) REFERENCES utente(id) ON DELETE SET NULL
);

-- Versioni dei piani di allenamento -->
CREATE TABLE IF NOT EXISTS training_plan_version (
  id INT AUTO_INCREMENT PRIMARY KEY,
  plan_id INT NOT NULL,
  version_number INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  content TEXT,
  created_by INT NULL,                -- NULLABLE per ON DELETE SET NULL
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tpver_plan
    FOREIGN KEY (plan_id) REFERENCES training_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_tpver_by
    FOREIGN KEY (created_by) REFERENCES utente(id) ON DELETE SET NULL
);

-- Assegnazioni dei piani di allenamento agli utenti
CREATE TABLE IF NOT EXISTS training_plan_assignment (
  id INT AUTO_INCREMENT PRIMARY KEY,
  plan_id INT NOT NULL,
  user_id INT NOT NULL,
  trainer_id INT NULL,                -- NULLABLE per ON DELETE SET NULL
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  active TINYINT(1) DEFAULT 1,
  notes TEXT,
  payment_id INT NULL,
  CONSTRAINT fk_ass_plan
    FOREIGN KEY (plan_id) REFERENCES training_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_ass_user
    FOREIGN KEY (user_id) REFERENCES utente(id) ON DELETE CASCADE,
  CONSTRAINT fk_ass_trainer
    FOREIGN KEY (trainer_id) REFERENCES utente(id) ON DELETE SET NULL
);

-- indici utili
CREATE INDEX idx_tp_created_by ON training_plan(created_by);
CREATE INDEX idx_tpver_plan ON training_plan_version(plan_id);
CREATE INDEX idx_ass_user ON training_plan_assignment(user_id);
CREATE INDEX idx_ass_trainer ON training_plan_assignment(trainer_id);
