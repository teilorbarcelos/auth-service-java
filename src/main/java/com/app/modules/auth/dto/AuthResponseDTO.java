package com.app.modules.auth.dto;

import java.util.List;
import java.util.Map;

public class AuthResponseDTO {
    private String message;
    private boolean valid;
    private String token;
    private String refreshToken;
    private UserData user;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public UserData getUser() { return user; }
    public void setUser(UserData user) { this.user = user; }

    public static class UserData {
        private String id;
        private String name;
        private String email;
        private RoleData role;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public RoleData getRole() { return role; }
        public void setRole(RoleData role) { this.role = role; }
    }

    public static class RoleData {
        private String id;
        private String name;
        private String description;
        private List<Map<String, Object>> permissions;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Map<String, Object>> getPermissions() { return permissions; }
        public void setPermissions(List<Map<String, Object>> permissions) { this.permissions = permissions; }
    }
}
