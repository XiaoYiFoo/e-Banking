package com.ebanking.dto;

import com.ebanking.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for paginated transaction list API.
 * 
 * Contains the list of transactions along with aggregated totals
 * and pagination information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated transaction response")
public class TransactionResponse {

    @Schema(description = "List of transactions for the current page")
    private List<Transaction> transactions;

    @Schema(description = "Total credit amount in base currency", example = "5000.00")
    private BigDecimal totalCredit;

    @Schema(description = "Total debit amount in base currency", example = "3000.00")
    private BigDecimal totalDebit;

    @Schema(description = "Base currency for totals", example = "GBP")
    private String baseCurrency;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Total number of transactions", example = "100")
    private long totalElements;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;
} 