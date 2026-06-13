package com.app.modules.feature;

import com.app.core.BaseEntity;
import jakarta.persistence.*;

/**
 * Feature entity — represents a system feature for ACL.
 * Equivalent to Feature.php
 */
@Entity
@Table(name = "features")
public class FeatureModel extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

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
}
