
CREATE TABLE users (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100),
    account_role VARCHAR(12) NOT NULL DEFAULT 'PLAYER'
        CHECK (account_role IN ('PLAYER', 'MANAGER', 'COMMISSIONER')),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE email_verification_codes (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(60) NOT NULL DEFAULT '',
    password_hash VARCHAR(100),
    account_role VARCHAR(12) NOT NULL DEFAULT 'PLAYER'
        CHECK (account_role IN ('PLAYER', 'MANAGER')),
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_verification_email
    ON email_verification_codes(email);

CREATE TABLE team_memberships (
    id VARCHAR(40) PRIMARY KEY,
    user_id VARCHAR(40) NOT NULL REFERENCES users(id),
    team_id VARCHAR(40) NOT NULL REFERENCES teams(id),
    role VARCHAR(10) NOT NULL
        CHECK (role IN ('PLAYER', 'MANAGER')),
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, team_id)
);

CREATE INDEX idx_memberships_user
    ON team_memberships(user_id);

CREATE INDEX idx_memberships_team
    ON team_memberships(team_id);

ALTER TABLE players ADD COLUMN user_id VARCHAR(40) REFERENCES users(id);

CREATE UNIQUE INDEX idx_players_user ON players(user_id);
