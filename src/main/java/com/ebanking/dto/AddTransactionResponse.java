package com.ebanking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for addTransaction API.
 *
 * Contains the result of adding a transaction via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add transaction response")
public class AddTransactionResponse {

    @Schema(description = "Response status",
            example = "success",
            allowableValues = {"success", "failed"})
    private String status;

    @Schema(description = "Response message",
            example = "Transaction created successfully")
    private String message;

    @Schema(description = "Unique transaction identifier",
            example = "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46")
    private String transactionId;

    @Schema(description = "Customer ID associated with the transaction",
            example = "P-0123456789")
    private String customerId;
}