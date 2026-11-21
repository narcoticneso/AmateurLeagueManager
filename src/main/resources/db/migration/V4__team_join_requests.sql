CREATE TABLE team_join_requests (
    id VARCHAR(40) PRIMARY KEY,
    user_id VARCHAR(40) NOT NULL REFERENCES users(id),
    team_id VARCHAR(40) NOT NULL REFERENCES teams(id),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'DENIED')),
    requested_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_join_requests_user ON team_join_requests(user_id, requested_at);
CREATE INDEX idx_join_requests_team ON team_join_requests(team_id, status);
