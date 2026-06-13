-- =============================================================================
-- Performance indexes
-- =============================================================================

-- 1. pg_trgm extension for fuzzy text search on LIKE '%word%' queries
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. IMMUTABLE wrapper for unaccent() - required for functional indexes
--    unaccent() is STABLE by default, but indexes need IMMUTABLE functions
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
  RETURNS text
  LANGUAGE sql
  IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT unaccent($1) $$;

-- 3. Composite B-tree indexes for common BaseEntity filtering + pagination
--    All queries filter by is_deleted=false and order by created_at DESC
CREATE INDEX IF NOT EXISTS idx_products_active_deleted_created
    ON products (is_deleted, active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_active_deleted_created
    ON users (is_deleted, active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_roles_active_deleted_created
    ON roles (is_deleted, active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_features_active_deleted_created
    ON features (is_deleted, active, created_at DESC);

-- 4. GIN trigram indexes for accent-insensitive search on searchable text fields
--    These support the LIKE '%word%' pattern used by SearchQueryBuilder.
--    The functional index matches the runtime expression: unaccent(lower(column))
CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
    ON products USING gin (immutable_unaccent(lower(sku)) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING gin (immutable_unaccent(lower(description)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_name_trgm
    ON users USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_email_trgm
    ON users USING gin (immutable_unaccent(lower(email)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_roles_name_trgm
    ON roles USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_roles_description_trgm
    ON roles USING gin (immutable_unaccent(lower(description)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_features_name_trgm
    ON features USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_features_description_trgm
    ON features USING gin (immutable_unaccent(lower(description)) gin_trgm_ops);
