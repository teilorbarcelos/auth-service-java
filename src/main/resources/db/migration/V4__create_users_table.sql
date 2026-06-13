-- Users table
CREATE TABLE users (
    id         VARCHAR(40) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(15),
    email      VARCHAR(255) NOT NULL,
    cognito_id VARCHAR(255),
    active     BOOLEAN DEFAULT TRUE,
    document   VARCHAR(20),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    avatar     VARCHAR(255),
    id_auth    VARCHAR(40),
    id_role    VARCHAR(40) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_cognito UNIQUE (cognito_id),
    CONSTRAINT uk_users_auth UNIQUE (id_auth),
    CONSTRAINT fk_users_auth FOREIGN KEY (id_auth) REFERENCES auth(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_users_role FOREIGN KEY (id_role) REFERENCES roles(id) ON DELETE RESTRICT ON UPDATE CASCADE
);
