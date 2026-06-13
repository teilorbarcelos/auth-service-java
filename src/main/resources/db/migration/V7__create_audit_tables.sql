CREATE SCHEMA IF NOT EXISTS audit;

-- Audit trail table
CREATE TABLE audit.tb_audit (
    id           VARCHAR(40) PRIMARY KEY,
    id_user      VARCHAR(255),
    user_name    VARCHAR(255),
    action_type  VARCHAR(255),
    execute_type VARCHAR(255),
    class_name   VARCHAR(255),
    function_name VARCHAR(255),
    params       TEXT,
    raw          TEXT,
    table_name   VARCHAR(255),
    diff_value   TEXT,
    error        TEXT,
    host         TEXT,
    ip           TEXT,
    base_url     TEXT,
    method       TEXT,
    hostname     TEXT,
    original_url TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Error log table
CREATE TABLE audit.tb_error_log (
    id            VARCHAR(40) PRIMARY KEY,
    id_user       VARCHAR(255),
    source        VARCHAR(255),
    error_message TEXT,
    error_data    TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
