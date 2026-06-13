package com.app.infrastructure.auth;

import java.lang.annotation.*;

/**
 * Marks a JAX-RS endpoint as requiring authentication.
 * The AuthFilter will intercept requests to annotated methods/classes.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authenticated {
}
