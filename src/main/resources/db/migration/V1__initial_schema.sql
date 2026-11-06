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
CREATE TABLE IF NOT EXISTS score_submissions (
  id VARCHAR(40) PRIMARY KEY,
  game_id VARCHAR(40) NOT NULL REFERENCES games(id),
  submitted_by VARCHAR(60) NOT NULL,
  submitted_home_score INTEGER NOT NULL CHECK (submitted_home_score BETWEEN 0 and 200),
  submitted_away_score INTEGER NOT NULL CHECK (submitted_away_score BETWEEN 0 and 200),
  disputed_home_score INTEGER CHECK (disputed_home_score BETWEEN 0 and 200),
  disputed_away_score INTEGER CHECK (disputed_away_score BETWEEN 0 and 200),
  status varchar(12) NOT NULL CHECK (status in ('PENDING', 'CONFIRMED', 'DISPUTED', 'RESOLVED')),
  submitted_at TIMESTAMP NOT NULL,
  confirmed_at TIMESTAMP,
  resolved_by VARCHAR(60)
);
