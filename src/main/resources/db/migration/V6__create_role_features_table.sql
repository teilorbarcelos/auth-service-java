-- Role-Feature pivot table (ACL permissions)
CREATE TABLE role_features (
    id_role    VARCHAR(40) NOT NULL,
    id_feature VARCHAR(40) NOT NULL,
    permissions JSONB,
    PRIMARY KEY (id_role, id_feature),
    CONSTRAINT fk_rf_role FOREIGN KEY (id_role) REFERENCES roles(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rf_feature FOREIGN KEY (id_feature) REFERENCES features(id) ON DELETE CASCADE ON UPDATE CASCADE
);
