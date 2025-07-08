package com.ebanking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for transaction list API.
 * 
 * Contains the parameters for filtering and paginating transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction list request parameters")
public class TransactionRequest {

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Schema(description = "Calendar month (1-12)", example = "10")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2014, message = "Year must be 2014 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    @Schema(description = "Calendar year", example = "2020")
    private Integer year;

    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 20;

    @Schema(description = "Base currency for totals", example = "GBP", defaultValue = "GBP")
    @Builder.Default
    private String baseCurrency = "GBP";
} 