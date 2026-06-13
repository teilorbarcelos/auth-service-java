package com.app.modules.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Error log entity.
 * Equivalent to ErrorLog.php
 */
@Entity
@Table(name = "tb_error_log", schema = "audit")
public class ErrorLog {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "id_user")
    private String idUser;

    private String source;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_data", columnDefinition = "text")
    private String errorData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorData() { return errorData; }
    public void setErrorData(String errorData) { this.errorData = errorData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
