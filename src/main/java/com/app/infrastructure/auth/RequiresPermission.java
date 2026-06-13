package com.app.infrastructure.auth;

import java.lang.annotation.*;

/**
 * Annotation for ACL-based permission checks.
 * Usage: @RequiresPermission(feature = "user", action = "view")
 * Equivalent to PermissionMiddleware.php
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String feature() default "";
    String action();
}
