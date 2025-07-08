package com.ebanking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response DTO for the e-Banking API.
 *
 * Used for all error responses including authentication, validation, and server errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "401")
    private int status;

    @Schema(description = "Error type/category", example = "Unauthorized")
    private String error;

    @Schema(description = "Human-readable error message", example = "Authentication is required to access this resource")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/v1/transactions")
    private String path;

    @Schema(description = "Timestamp when the error occurred")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Correlation ID for tracking the request", example = "req-12345")
    private String correlationId;

    @Schema(description = "List of validation errors (if applicable)")
    private List<ValidationError> validationErrors;

    /**
     * Nested class for validation errors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation error details")
    public static class ValidationError {

        @Schema(description = "Field name that failed validation", example = "amount")
        private String field;

        @Schema(description = "Validation error message", example = "Amount must be greater than 0")
        private String message;

        @Schema(description = "Rejected value", example = "-100")
        private Object rejectedValue;
    }

    /**
     * Static factory method for creating a basic error response.
     */
    public static ErrorResponse of(int status, String error, String message) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Static factory method for creating an error response with path.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Static factory method for creating an error response with correlation ID.
     */
    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}