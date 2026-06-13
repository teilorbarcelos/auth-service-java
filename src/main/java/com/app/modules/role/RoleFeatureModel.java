package com.app.modules.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Role-Feature pivot entity with JSONB permissions.
 * Equivalent to the role_features pivot table in PHP.
 */
@Entity
@Table(name = "role_features")
@IdClass(RoleFeatureId.class)
public class RoleFeatureModel {

    @Id
    @Column(name = "id_role")
    @JsonProperty("id_role")
    private String idRole;

    @Id
    @Column(name = "id_feature")
    @JsonProperty("id_feature")
    private String idFeature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @JsonIgnore
    private String permissions;

    @Transient
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("create")
    public boolean isCreate() {
        return getPermission("create");
    }

    @JsonProperty("view")
    public boolean isView() {
        return getPermission("view");
    }

    @JsonProperty("delete")
    public boolean isDelete() {
        return getPermission("delete");
    }

    @JsonProperty("activate")
    public boolean isActivate() {
        return getPermission("activate");
    }

    private boolean getPermission(String key) {
        try {
            if (permissions == null || permissions.isBlank())
                return false;
            return mapper.readTree(permissions).path(key).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", insertable = false, updatable = false)
    @JsonIgnore
    private RoleModel role;

    public String getIdRole() {
        return idRole;
    }

    public void setIdRole(String idRole) {
        this.idRole = idRole;
    }

    public String getIdFeature() {
        return idFeature;
    }

    public void setIdFeature(String idFeature) {
        this.idFeature = idFeature;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public RoleModel getRole() {
        return role;
    }

    public void setRole(RoleModel role) {
        this.role = role;
    }
}
