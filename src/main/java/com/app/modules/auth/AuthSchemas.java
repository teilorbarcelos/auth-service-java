package com.app.modules.auth;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AuthSchemas {

    @Schema(name = "LoginRequest")
    public static class LoginRequest {
        @Schema(required = true, examples = {"admin@email.com"}, description = "E-mail do usuário")
        public String email;
        
        @Schema(required = true, examples = {"admin123"}, description = "Senha de acesso")
        public String password;
    }

    @Schema(name = "AuthResponse")
    public static class AuthResponse {
        @Schema(description = "Token JWT de acesso")
        public String token;
        
        @Schema(examples = {"3600"}, description = "Tempo de expiração em segundos")
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

    @Schema(name = "RefreshTokenRequest")
    public static class RefreshTokenRequest {
        @Schema(required = true, examples = {"uuid-do-refresh-token"})
        public String refreshToken;
    }

    @Schema(name = "PasswordRequest")
    public static class PasswordRequest {
        @Schema(required = true, examples = {"user@email.com"})
        public String email;
    }
}
