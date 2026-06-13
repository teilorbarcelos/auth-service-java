-- Roles table
CREATE TABLE roles (
    id          VARCHAR(40) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN DEFAULT TRUE,
    is_deleted  BOOLEAN DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
