package com.app.modules.role;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public class RoleSchemas {

    @Schema(name = "RoleResponse")
    @RegisterForReflection
    public static class ResponseDto {
        public String id;
        public String name;
        public String description;
        public Boolean active;
        public LocalDateTime createdAt;
        public List<RoleFeatureResponse> features;
    }

    @Schema(name = "RoleListResponse")
    @RegisterForReflection
    public static class ListResponseDto {
        public List<ResponseDto> items;
        @Schema(examples = {"100"})
        public Long total;
        @Schema(examples = {"0"})
        public Integer page;
        @Schema(examples = {"25"})
        public Integer size;
    }

    @Schema(name = "RoleRequest")
    @RegisterForReflection
    public static class RequestDto {
        @Schema(required = true, examples = {"administrator"})
        public String name;
        @Schema(examples = {"Acesso total ao sistema"})
        public String description;
        @Schema(examples = {"true"})
        public Boolean active;
        @Schema(description = "Lista de permissões por feature")
        public List<PermissionRequest> permissions;
    }

    @Schema(name = "PermissionRequest")
    public static class PermissionRequest {
        @Schema(required = true, examples = {"user"})
        public String id_feature;
        public Boolean create;
        public Boolean view;
        public Boolean delete;
        public Boolean activate;
    }

    @Schema(name = "RoleFeatureResponse")
    public static class RoleFeatureResponse {
        public String idFeature;
        public String permissions;
    }

    @Schema(name = "RoleStatusRequest")
    @RegisterForReflection
    public record StatusRequest(
        @Schema(required = true, examples = {"true"})
        Boolean active
    ) {}
}
