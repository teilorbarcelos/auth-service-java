package com.app.modules.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User authentication entity — stores password hash and reset tokens.
 * Equivalent to UserAuth.php
 */
@Entity
@Table(name = "auth")
public class AuthModel {

    @Id
    @Column(length = 40)
    private String id;

    private String password;

    @Column(name = "request_password_token")
    private String requestPasswordToken;

    @Column(name = "request_password_expiration")
    private LocalDateTime requestPasswordExpiration;

    private Integer retries = 0;

    @Column(name = "first_access")
    private Boolean firstAccess = true;

    @Column(name = "session_version")
    private Integer sessionVersion = 1;

    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRequestPasswordToken() {
        return requestPasswordToken;
    }

    public void setRequestPasswordToken(String token) {
        this.requestPasswordToken = token;
    }

    public LocalDateTime getRequestPasswordExpiration() {
        return requestPasswordExpiration;
    }

    public void setRequestPasswordExpiration(LocalDateTime exp) {
        this.requestPasswordExpiration = exp;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Boolean getFirstAccess() {
        return firstAccess;
    }

    public void setFirstAccess(Boolean firstAccess) {
        this.firstAccess = firstAccess;
    }

    public Integer getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
