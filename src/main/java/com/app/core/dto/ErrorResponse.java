package com.app.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response DTO.
 * Matches the PHP format: {success: false, error: {message, code, details}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private ErrorDetail error;

    public ErrorResponse() {}

    public ErrorResponse(String message, String code) {
        this.error = new ErrorDetail(message, code, null);
    }

    public ErrorResponse(String message, String code, Object details) {
        this.error = new ErrorDetail(message, code, details);
    }

    public boolean isSuccess() { return success; }
    public ErrorDetail getError() { return error; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String message;
        private String code;
        private Object details;

        public ErrorDetail() {}

        public ErrorDetail(String message, String code, Object details) {
            this.message = message;
            this.code = code;
            this.details = details;
        }

        public String getMessage() { return message; }
        public String getCode() { return code; }
        public Object getDetails() { return details; }
    }
}
