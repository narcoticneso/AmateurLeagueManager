
CREATE TABLE users (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE email_verification_codes (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
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
