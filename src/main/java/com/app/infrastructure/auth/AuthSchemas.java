package com.app.infrastructure.auth;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AuthSchemas {

    @Schema(name = "LoginRequest")
    public static class LoginRequest {
        @Schema(required = true, examples = "admin@email.com")
        public String email;

        @Schema(required = true, examples = "admin123")
        public String password;
    }

    @Schema(name = "AuthResponse")
    public static class AuthResponse {
        @Schema(examples = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        public String token;

        @Schema(examples = "3600")
        public Long expiresIn;

        public UserInfo user;
    }

    @Schema(name = "UserInfo")
    public static class UserInfo {
        public String id;
        public String name;
        public String email;
        public String role;
    }

    @Schema(name = "MessageResponse")
    public static class MessageResponse {
        public String message;
    }
}
