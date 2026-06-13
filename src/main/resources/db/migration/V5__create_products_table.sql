-- Products table
CREATE TABLE products (
    id          VARCHAR(40) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    sku         VARCHAR(100) NOT NULL,
    category    VARCHAR(255),
    price       DECIMAL(10,2) NOT NULL,
    stock       INTEGER DEFAULT 0,
    description TEXT,
    active      BOOLEAN DEFAULT TRUE,
    is_deleted  BOOLEAN DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_sku UNIQUE (sku)
);
