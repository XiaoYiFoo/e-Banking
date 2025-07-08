package com.ebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model representing a money account transaction.
 * 
 * This entity contains all the essential information about a financial transaction
 * including amount, currency, account details, and metadata.
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Money account transaction")
public class Transaction {

    @Id
    @NotBlank(message = "Transaction ID is required")
    @Size(min = 36, max = 36, message = "Transaction ID must be exactly 36 characters")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
             message = "Transaction ID must be a valid UUID")
    @Schema(description = "Unique transaction identifier (UUID)", 
            example = "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46")
    private String id;

    @NotNull(message = "Amount is required")
    @Schema(description = "Transaction amount", example = "100.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter currency code")
    @Schema(description = "Transaction currency (ISO 4217)", example = "GBP")
    private String currency;

    @NotBlank(message = "Account IBAN is required")
    @Schema(description = "Account IBAN", example = "CH93-0000-0000-0000-0000-0")
    private String accountIban;

    @NotNull(message = "Value date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Transaction value date", example = "2020-10-01")
    private LocalDate valueDate;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Transaction description", example = "Online payment CHF")
    private String description;

    @Schema(description = "Customer ID associated with this transaction", 
            example = "P-0123456789")
    @JsonProperty("customerId")
    private String customerId;

    /**
     * Determines if this transaction is a credit (positive amount).
     * 
     * @return true if amount is positive, false otherwise
     */
    public boolean isCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Determines if this transaction is a debit (negative amount).
     * 
     * @return true if amount is negative, false otherwise
     */
    public boolean isDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Gets the absolute amount for calculations.
     * 
     * @return absolute value of the amount
     */
    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
} 