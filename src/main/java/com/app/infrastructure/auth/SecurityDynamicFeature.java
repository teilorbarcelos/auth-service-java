package com.app.infrastructure.auth;

import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;

/**
 * Dynamically registers PermissionFilter only on methods annotated
 * with @RequiresPermission.
 */
@Provider
public class SecurityDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> clazz = resourceInfo.getResourceClass();

        RequiresPermission permission = findRequiresPermission(method);

        if (permission != null) {
            String feature = permission.feature();

            if (feature.isEmpty() && clazz.isAnnotationPresent(ResourceFeature.class)) {
                feature = clazz.getAnnotation(ResourceFeature.class).value();
            }

            if (!feature.isEmpty()) {
                context.register(new PermissionFilter(feature, permission.action()),
                        jakarta.ws.rs.Priorities.AUTHORIZATION);
            }
        }
    }

    private RequiresPermission findRequiresPermission(Method method) {
        if (method == null)
            return null;

        if (method.isAnnotationPresent(RequiresPermission.class)) {
            return method.getAnnotation(RequiresPermission.class);
        }

        Class<?> declaringClass = method.getDeclaringClass();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        Class<?> superclass = declaringClass.getSuperclass();
        while (superclass != null) {
            if (superclass.equals(Object.class)) {
                break;
            }
            RequiresPermission p = findInClass(superclass, methodName, parameterTypes);
            if (p != null) {
                return p;
            }
            superclass = superclass.getSuperclass();
        }

        for (Class<?> iface : declaringClass.getInterfaces()) {
            RequiresPermission p = findInClass(iface, methodName, parameterTypes);
            if (p != null) {
                return p;
            }
        }

        return null;
    }

    private RequiresPermission findInClass(Class<?> clazz, String name, Class<?>[] params) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == params.length) {
                boolean match = true;
                Class<?>[] mParams = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (!mParams[i].isAssignableFrom(params[i])) {
                        match = false;
                        break;
                    }
                }
                if (match && m.isAnnotationPresent(RequiresPermission.class)) {
                    return m.getAnnotation(RequiresPermission.class);
                }
            }
        }
        return null;
    }
}
