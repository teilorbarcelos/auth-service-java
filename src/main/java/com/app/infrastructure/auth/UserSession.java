package com.app.infrastructure.auth;

import jakarta.enterprise.context.RequestScoped;
import java.util.*;

/**
 * Request-scoped user session holder.
 * Equivalent to UserSession.php but thread-safe via CDI @RequestScoped.
 */
@RequestScoped
public class UserSession {

    private String userId;
    private Map<String, Object> user;

    public void setUser(Map<String, Object> claims) {
        if (claims == null) {
            this.user = null;
            this.userId = null;
            return;
        }

        Object rawId = claims.get("uid");
        if (rawId != null) {
            this.userId = rawId.toString();
        }

        this.user = new HashMap<>(claims);

        Object roleId = claims.get("roleId");
        if (roleId != null) {
            this.user.put("id_role", roleId.toString());
        }
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public boolean hasPermission(String feature, String action) {
        if (user == null) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        if (!user.containsKey("permissions") || user.get("permissions") == null) {
            return false;
        }

        Object permissionsObj = user.get("permissions");
        if (permissionsObj instanceof List<?> permissions) {
            for (Object p : permissions) {
                if (p instanceof Map<?, ?> perm) {
                    if (feature.equals(perm.get("feature"))) {
                        Object actionValue = perm.get(action);
                        return Boolean.TRUE.equals(actionValue);
                    }
                }
            }
        }

        return false;
    }

    public boolean isAdmin() {
        if (user == null)
            return false;
        Object roleId = user.get("id_role");
        if (roleId == null)
            roleId = user.get("roleId");
        if (roleId == null)
            return false;

        String roleStr = roleId.toString();
        return "admin".equals(roleStr) || "administrator".equals(roleStr);
    }
}
