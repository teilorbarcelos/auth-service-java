package com.app.modules.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_audit", schema = "audit")
public class AuditModel {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "id_user")
    private String idUser;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "execute_type")
    private String executeType;

    @Column(name = "class_name")
    private String className;

    @Column(name = "function_name")
    private String functionName;

    @Column(columnDefinition = "text")
    private String params;

    @Column(columnDefinition = "text")
    private String raw;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "diff_value", columnDefinition = "text")
    private String diffValue;

    @Column(columnDefinition = "text")
    private String error;

    @Column(columnDefinition = "text")
    private String host;

    @Column(columnDefinition = "text")
    private String ip;

    @Column(name = "base_url", columnDefinition = "text")
    private String baseUrl;

    @Column(columnDefinition = "text")
    private String method;

    @Column(columnDefinition = "text")
    private String hostname;

    @Column(name = "original_url", columnDefinition = "text")
    private String originalUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.id == null)
            this.id = UUID.randomUUID().toString();
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getExecuteType() {
        return executeType;
    }

    public void setExecuteType(String executeType) {
        this.executeType = executeType;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDiffValue() {
        return diffValue;
    }

    public void setDiffValue(String diffValue) {
        this.diffValue = diffValue;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
