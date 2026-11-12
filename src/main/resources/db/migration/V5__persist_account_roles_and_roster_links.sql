ALTER TABLE users ADD COLUMN account_role VARCHAR(12) NOT NULL DEFAULT 'PLAYER'
    CHECK (account_role IN ('PLAYER', 'MANAGER', 'COMMISSIONER'));

ALTER TABLE players ADD COLUMN user_id VARCHAR(40) REFERENCES users(id);

ALTER TABLE email_verification_codes ADD COLUMN account_role VARCHAR(12) NOT NULL DEFAULT 'PLAYER'
    CHECK (account_role IN ('PLAYER', 'MANAGER'));

CREATE UNIQUE INDEX idx_players_user ON players(user_id);
