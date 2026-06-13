package com.app.modules.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public class UserSchemas {

    @Schema(name = "UserResponse")
    @RegisterForReflection
    public static class ResponseDto {
        public String id;
        public String name;
        public String email;
        public String idRole;
        public Boolean active;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @Schema(name = "UserListResponse")
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

    @Schema(name = "UserRequest")
    @RegisterForReflection
    public static class RequestDto {
        @Schema(required = true, examples = {"João Silva"})
        public String name;
        @Schema(required = true, examples = {"joao@email.com"})
        public String email;
        @Schema(required = true, description = "UUID do Papel")
        public String idRole;
        @Schema(examples = {"true"})
        public Boolean active;
    }

    @Schema(name = "UserStatusRequest")
    @RegisterForReflection
    public record StatusRequest(
        @Schema(required = true, examples = {"true"})
        Boolean active
    ) {}
}
