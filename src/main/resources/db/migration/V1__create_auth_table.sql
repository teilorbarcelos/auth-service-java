-- Auth table (stores passwords and password reset tokens)
CREATE TABLE auth (
    id         VARCHAR(40) PRIMARY KEY,
    password   VARCHAR(255),
    request_password_token      VARCHAR(255),
    request_password_expiration TIMESTAMP,
    retries      INTEGER DEFAULT 0,
    first_access BOOLEAN DEFAULT TRUE,
    active       BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
