package {{package}};

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Schemas para o módulo {{module_name}}.
 * Concentra as definições de DTOs para manter o Model e o Resource limpos.
 */
public class {{module_name}}Schemas {

    @Schema(name = "{{module_name}}Response")
    @RegisterForReflection
    public static class ResponseDto {
        public String id;
{{schema_fields}}
        public Boolean active;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @Schema(name = "{{module_name}}ListResponse")
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

    @Schema(name = "{{module_name}}Request")
    @RegisterForReflection
    public static class RequestDto {
{{schema_fields}}
        @Schema(examples = {"true"})
        public Boolean active;
    }

    @Schema(name = "{{module_name}}StatusRequest")
    @RegisterForReflection
    public record StatusRequest(
        @Schema(required = true, examples = {"true"})
        Boolean active
    ) {}
}
