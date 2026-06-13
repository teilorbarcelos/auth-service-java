package com.app.modules.role;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for RoleFeature.
 */
public class RoleFeatureId implements Serializable {
    public String idRole;
    public String idFeature;

    public RoleFeatureId() {}

    public RoleFeatureId(String idRole, String idFeature) {
        this.idRole = idRole;
        this.idFeature = idFeature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleFeatureId that)) return false;
        return Objects.equals(idRole, that.idRole) && Objects.equals(idFeature, that.idFeature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRole, idFeature);
    }
}
