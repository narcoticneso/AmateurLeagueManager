CREATE TABLE IF NOT EXISTS teams (
  id VARCHAR(40) PRIMARY KEY,
  name VARCHAR(60) NOT NULL UNIQUE,
  abbreviation VARCHAR(3) NOT NULL,
  color VARCHAR(7) NOT NULL,
  manager VARCHAR(60) NOT NULL
);
CREATE TABLE IF NOT EXISTS players (
  id VARCHAR(40) PRIMARY KEY,
  team_id VARCHAR(40) NOT NULL REFERENCES teams(id),
  name VARCHAR(60) NOT NULL,
  position VARCHAR(3) NOT NULL,
  jersey_number INTEGER NOT NULL CHECK (jersey_number BETWEEN 0 AND 99),
  UNIQUE(team_id, jersey_number)
);
CREATE TABLE IF NOT EXISTS games (
  id VARCHAR(40) PRIMARY KEY,
  home_id VARCHAR(40) NOT NULL REFERENCES teams(id),
  away_id VARCHAR(40) NOT NULL REFERENCES teams(id),
  kickoff VARCHAR(30) NOT NULL,
  location VARCHAR(100) NOT NULL,
  week INTEGER NOT NULL CHECK (week BETWEEN 1 AND 30),
  phase VARCHAR(12) NOT NULL,
  status VARCHAR(12) NOT NULL,
  home_score INTEGER,
  away_score INTEGER,
  CHECK(home_id <> away_id)
);
