CREATE TABLE IF NOT EXISTS allowed_emails (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_allowed_emails_email
    ON allowed_emails (email);
