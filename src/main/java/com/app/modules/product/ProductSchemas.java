package com.app.modules.product;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductSchemas {

    @Schema(name = "ProductResponse")
    @RegisterForReflection
    public static class ResponseDto {
        public String id;
        public String name;
        public String description;
        public BigDecimal price;
        public Integer stock;
        public Boolean active;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @Schema(name = "ProductListResponse")
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

    @Schema(name = "ProductRequest")
    @RegisterForReflection
    public static class RequestDto {
        @Schema(required = true, examples = {"Cerveja Artesanal"})
        public String name;
        @Schema(examples = {"IPA 500ml"})
        public String description;
        @Schema(examples = {"15.90"})
        public BigDecimal price;
        @Schema(examples = {"100"})
        public Integer stock;
        @Schema(examples = {"true"})
        public Boolean active;
    }

    @Schema(name = "ProductStatusRequest")
    @RegisterForReflection
    public record StatusRequest(
        @Schema(required = true, examples = {"true"})
        Boolean active
    ) {}
}
