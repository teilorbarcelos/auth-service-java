package com.app.modules.role;

import com.app.core.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Role entity with many-to-many relationship to Features via RoleFeature.
 * Equivalent to Role.php
 */
@Entity
@Table(name = "roles")
public class RoleModel extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonProperty("RoleFeature")
    private List<RoleFeatureModel> roleFeatures = new ArrayList<>();

    @Transient
    private List<Map<String, Object>> permissions;

    public List<Map<String, Object>> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Map<String, Object>> permissions) {
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RoleFeatureModel> getRoleFeatures() {
        return roleFeatures;
    }

    public void setRoleFeatures(List<RoleFeatureModel> roleFeatures) {
        this.roleFeatures = roleFeatures;
    }
}
