package com.ebanking.controller;

import com.ebanking.domain.Transaction;
import com.ebanking.dto.AddTransactionResponse;
import com.ebanking.dto.ErrorResponse;
import com.ebanking.dto.TransactionRequest;
import com.ebanking.dto.TransactionResponse;
import com.ebanking.service.KafkaTransactionProducer;
import com.ebanking.service.TransactionService;
import com.ebanking.validation.NotZero;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for transaction retrieval.
 *
 * Provides a paginated endpoint for retrieving transactions for the authenticated customer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated  // ← Add this annotation
@Tag(name = "Transactions", description = "API for retrieving paginated money account transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final KafkaTransactionProducer kafkaProducer;

    @Operation(
            summary = "Get paginated list of transactions for the authenticated customer",
            description = "Returns a paginated list of money account transactions for the given month and year, " +
                    "including total credit and debit values at the current exchange rate."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieve transactions",
                    content = @Content(
                            schema = @Schema(implementation = TransactionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "BadRequest",
                                    value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid value for parameter 'month': 'Jan'. Expected type: Integer\", \"path\": \"/api/v1/getTransaction\", \"timestamp\": \"2024-06-01 12:00:00\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to retrieve transactions",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InternalServerError",
                                    value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Failed to get transaction from Kafka\", \"path\": \"/api/v1/getTransaction\", \"timestamp\": \"2024-06-01 12:00:00\" }"
                            )
                    )
            )
    })
    @GetMapping("/getTransaction")
    public ResponseEntity<TransactionResponse> getTransactions(
            @Parameter(description = "Calendar month (1-12)", required = true)
            @RequestParam("month")
            @NotNull(message = "Month is required")
            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            Integer month,

            @Parameter(description = "Calendar year", required = true)
            @RequestParam("year")
            @NotNull(message = "Year is required")
            @Min(value = 2020, message = "Year must be 2020 or later")
            @Max(value = 2030, message = "Year must be 2030 or earlier")
            Integer year,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            Integer page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            Integer size,

            @Parameter(description = "Base currency for totals (ISO 4217)", example = "GBP")
            @RequestParam(value = "baseCurrency", defaultValue = "GBP")
            @NotBlank(message = "Base currency cannot be empty")
            @Size(min = 3, max = 3, message = "Base currency must be exactly 3 characters")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be a valid 3-letter currency code (ISO 4217)")
            String baseCurrency
    ) {
        // Get the authenticated customer ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String customerId = authentication.getName();

        try {
            TransactionRequest request = TransactionRequest.builder()
                    .month(month)
                    .year(year)
                    .page(page)
                    .size(size)
                    .baseCurrency(baseCurrency)
                    .build();

            // Retrieve paginated transactions from the database
            TransactionResponse response = transactionService.getTransactions(customerId, request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve transactions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transactions", e);
        }
    }

    @PostMapping("/addTransaction")
    @Operation(
            summary = "Add transaction via Kafka",
            description = "Creates a new transaction and sends it to Kafka topic for processing"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Transaction created successfully",
                    content = @Content(
                            schema = @Schema(implementation = AddTransactionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "BadRequest",
                                    value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid value for parameter 'amount': 'one hundred'. Expected type: BigDecimal\", \"path\": \"/api/v1/transactions\", \"timestamp\": \"2024-06-01 12:00:00\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to send transaction to Kafka",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InternalServerError",
                                    value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Failed to send transaction to Kafka\", \"path\": \"/api/v1/transactions\", \"timestamp\": \"2024-06-01 12:00:00\" }"
                            )
                    )
            )
    })
    public ResponseEntity<AddTransactionResponse> addTransaction(
            Authentication authentication,
            @Parameter(description = "Transaction amount", required = true)
            @RequestParam
            @NotZero
            @NotNull(message = "Amount is required")
            BigDecimal amount,

            @Parameter(description = "Transaction currency")
            @RequestParam(defaultValue = "USD")
            @NotBlank(message = "Currency cannot be empty")
            @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter currency code")
            String currency,

            @Parameter(description = "Account IBAN")
            @RequestParam(defaultValue = "CH93-0000-0000-0000-0000-0")
            @NotBlank(message = "Account IBAN cannot be empty")
            String accountIban,

            @Parameter(description = "Transaction description")
            @RequestParam(defaultValue = "Online payment")
            @NotBlank(message = "Description cannot be empty")
            @Size(max = 255, message = "Description cannot exceed 255 characters")
            String description,

            @Parameter(description = "Value date (yyyy-MM-dd format)")
            @RequestParam(value = "valueDate", required = false)
            @Pattern(regexp = "^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
                    message = "Value date must be in yyyy-MM-dd format (e.g., 2024-07-15)")
            String valueDateStr
    ) {
        String customerId = authentication.getName();

        log.info("Adding transaction for customer: {}, amount: {} {}, description: {}",
                customerId, amount, currency, description);

        // Parse value date or use current date
        LocalDate valueDate = valueDateStr != null ?
                LocalDate.parse(valueDateStr) : LocalDate.now();

        // Create transaction
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(amount)
                .currency(currency)
                .accountIban(accountIban)
                .valueDate(valueDate)
                .description(description)
                .customerId(customerId)
                .build();

        try {
            // Send transaction to Kafka
            kafkaProducer.sendTransactionSync(transaction);

            AddTransactionResponse response = AddTransactionResponse.builder()
                    .message("Transaction created successfully")
                    .transactionId(transaction.getId())
                    .status("success")
                    .customerId(customerId)
                    .build();

            log.info("Transaction sent to Kafka successfully: {}", transaction.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to send transaction to Kafka: {}", e.getMessage(), e);

            AddTransactionResponse response = AddTransactionResponse.builder()
                    .status("failed")
                    .message("Failed to send transaction to Kafka")
                    .customerId(customerId)
                    .transactionId(transaction.getId())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}