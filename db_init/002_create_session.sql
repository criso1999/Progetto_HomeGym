-- ./db_init/002_create_sessions.sql
CREATE TABLE IF NOT EXISTS session (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  trainer VARCHAR(100) NOT NULL,
  scheduled_at DATETIME NOT NULL,
  duration_minutes INT DEFAULT 60,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES utente(id) ON DELETE SET NULL
);

-- indice utile per ricerca per trainer/data
CREATE INDEX IF NOT EXISTS idx_session_trainer ON session(trainer);
CREATE INDEX IF NOT EXISTS idx_session_scheduled_at ON session(scheduled_at);
