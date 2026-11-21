ALTER TABLE users ADD COLUMN commissioner BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET commissioner = TRUE WHERE account_role = 'COMMISSIONER';

ALTER TABLE users DROP COLUMN account_role;

ALTER TABLE email_verification_codes DROP COLUMN account_role;
