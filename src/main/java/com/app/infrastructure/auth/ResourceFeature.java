package com.app.infrastructure.auth;

import java.lang.annotation.*;

/**
 * Defines the feature name for a Resource class.
 * Used by AuthFilter to resolve @RequiresPermission without explicit feature name.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceFeature {
    String value();
}
