ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100);

ALTER TABLE email_verification_codes
    ADD COLUMN password_hash VARCHAR(100);