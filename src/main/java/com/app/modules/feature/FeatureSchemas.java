package com.app.modules.feature;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

public class FeatureSchemas {

    @Schema(name = "FeatureResponse")
    @RegisterForReflection
    public static class ResponseDto {
        public String id;
        public String name;
        public Boolean active;
    }

    @Schema(name = "FeatureListResponse")
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

    @Schema(name = "FeatureStatusRequest")
    @RegisterForReflection
    public record StatusRequest(
        @Schema(required = true, examples = {"true"})
        Boolean active
    ) {}
}
