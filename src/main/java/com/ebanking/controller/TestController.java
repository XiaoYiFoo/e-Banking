package com.ebanking.controller;

import com.ebanking.domain.Transaction;
import com.ebanking.service.KafkaTransactionProducer;
import com.ebanking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test Controller for development and testing purposes.
 * 
 * Provides various test endpoints for:
 * - Adding test transactions
 * - Health checks
 * - Service status verification
 * - Sample data generation
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "Test", description = "Test endpoints for development and debugging")
public class TestController {

    private final TransactionService transactionService;
    private final KafkaTransactionProducer kafkaProducer;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "e-Banking Transaction Service");
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Service information", description = "Returns information about the service")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "e-Banking Transaction Service");
        response.put("version", "1.0.0");
        response.put("description", "REST API microservice for e-Banking portal transactions");
        response.put("features", new String[]{
                "JWT Authentication",
                "Transaction Management",
                "Exchange Rate Conversion",
                "Pagination Support",
                "OpenAPI Documentation"
        });
        response.put("endpoints", new String[]{
                "GET /api/v1/transactions - Get paginated transactions",
                "POST /test/transactions - Add test transactions",
                "GET /test/health - Health check",
                "GET /swagger-ui/ - API documentation"
        });

        return ResponseEntity.ok(response);
    }

//    @PostMapping("/transactions")
//    @Operation(summary = "Add test transactions", description = "Adds sample transactions for testing purposes")
//    public ResponseEntity<Map<String, Object>> addTestTransactions(
//            @Parameter(description = "Customer ID for the transactions", example = "customer123")
//            @RequestParam(defaultValue = "customer123") String customerId,
//            @Parameter(description = "Number of transactions to add", example = "5")
//            @RequestParam(defaultValue = "5") int count) {
//
//        log.info("Adding {} test transactions for customer: {}", count, customerId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Test transactions added successfully");
//        response.put("customerId", customerId);
//        response.put("count", count);
//        response.put("transactions", new HashMap<>());
//
//        int successCount = 0;
//
//        // Add transactions directly to service
//        for (int i = 1; i <= count; i++) {
//            var transaction = Transaction.builder()
//                    .id(UUID.randomUUID().toString())
//                    .amount(BigDecimal.valueOf(100 * i * (i % 2 == 0 ? -1 : 1)))
//                    .currency(i % 2 == 0 ? "CHF" : "GBP")
//                    .accountIban("CH93-0000-0000-0000-0000-" + i)
//                    .valueDate(LocalDate.of(2020, 10, i))
//                    .description("Test transaction " + i)
//                    .customerId(customerId)
//                    .build();
//
//            boolean success = transactionService.addTransaction(transaction);
//            if (success) {
//                successCount++;
//
//                // Add transaction details to response
//                Map<String, Object> txInfo = new HashMap<>();
//                txInfo.put("id", transaction.getId());
//                txInfo.put("amount", transaction.getAmount());
//                txInfo.put("currency", transaction.getCurrency());
//                txInfo.put("description", transaction.getDescription());
//                response.put("transaction_" + i, txInfo);
//            }
//        }
//
//        response.put("successCount", successCount);
//        response.put("failedCount", count - successCount);
//
//        return ResponseEntity.ok(response);
//    }

    // @PostMapping("/kafka/send-single")
    // @Operation(summary = "Send single transaction to Kafka", description = "Sends a single transaction to Kafka topic")
    // public ResponseEntity<Map<String, Object>> sendSingleTransactionToKafka(
    //         @RequestParam(defaultValue = "customer123") String customerId,
    //         @RequestParam(defaultValue = "500.00") BigDecimal amount,
    //         @RequestParam(defaultValue = "USD") String currency,
    //         @RequestParam(defaultValue = "Online purchase") String description) {

    //     var transaction = Transaction.builder()
    //             .id(UUID.randomUUID().toString())
    //             .amount(amount)
    //             .currency(currency)
    //             .accountIban("CH93-0000-0000-0000-0000-1")
    //             .valueDate(LocalDate.now())
    //             .description(description)
    //             .customerId(customerId)
    //             .build();

    //     kafkaProducer.sendTransaction(transaction);

    //     Map<String, Object> response = new HashMap<>();
    //     response.put("message", "Single transaction sent to Kafka successfully");
    //     response.put("transactionId", transaction.getId());

    //     return ResponseEntity.ok(response);
    // }

//    @PostMapping("/kafka/send-single")
//    @Operation(summary = "Send single transaction to Kafka", description = "Sends a single transaction to Kafka topic")
//    public ResponseEntity<Map<String, Object>> sendSingleTransactionToKafka(
//            Authentication authentication, // Inject the authenticated user
//            @RequestParam(defaultValue = "50.00") BigDecimal amount,
//            @RequestParam(defaultValue = "USD") String currency,
//            @RequestParam(defaultValue = "Online purchase") String description) {
//
//        String customerId = authentication.getName(); // Extract from JWT
//
//        var transaction = Transaction.builder()
//                .id(UUID.randomUUID().toString())
//                .amount(amount)
//                .currency(currency)
//                .accountIban("CH93-0000-0000-0000-0000-1")
//                .valueDate(LocalDate.now())
//                .description(description)
//                .customerId(customerId)
//                .build();
//
//        kafkaProducer.sendTransaction(transaction);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Single transaction sent to Kafka successfully");
//        response.put("transactionId", transaction.getId());
//
//        return ResponseEntity.ok(response);
//    }

//    @PostMapping("/transactions/bulk")
//    @Operation(summary = "Add bulk test transactions", description = "Adds multiple test transactions with different currencies")
//    public ResponseEntity<Map<String, Object>> addBulkTestTransactions(
//            @Parameter(description = "Customer ID for the transactions", example = "customer123")
//            @RequestParam(defaultValue = "customer123") String customerId) {
//
//        log.info("Adding bulk test transactions for customer: {}", customerId);
//
//        // Create transactions with different currencies
//        String[] currencies = {"USD", "EUR", "GBP", "CHF", "JPY"};
//        String[] descriptions = {
//            "Salary payment", "Online purchase", "ATM withdrawal",
//            "Bank transfer", "Utility bill payment", "Restaurant payment",
//            "Gas station", "Grocery store", "Insurance premium", "Tax payment"
//        };
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Bulk test transactions added successfully");
//        response.put("customerId", customerId);
//        response.put("totalTransactions", currencies.length * 2);
//
//        int transactionCount = 0;
//        int successCount = 0;
//
//        for (String currency : currencies) {
//            for (int i = 1; i <= 2; i++) {
//                transactionCount++;
//                var transaction = Transaction.builder()
//                        .id(UUID.randomUUID().toString())
//                        .amount(BigDecimal.valueOf(500 * i * (i % 2 == 0 ? -1 : 1)))
//                        .currency(currency)
//                        .accountIban("CH93-0000-0000-0000-0000-" + transactionCount)
//                        .valueDate(LocalDate.of(2020, 10, transactionCount))
//                        .description(descriptions[transactionCount % descriptions.length])
//                        .customerId(customerId)
//                        .build();
//
//                boolean success = transactionService.addTransaction(transaction);
//                if (success) {
//                    successCount++;
//                }
//            }
//        }
//
//        response.put("successCount", successCount);
//        response.put("failedCount", (currencies.length * 2) - successCount);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/transactions/count")
//    @Operation(summary = "Get transaction count", description = "Returns the total number of transactions for a customer")
//    public ResponseEntity<Map<String, Object>> getTransactionCount(
//            @Parameter(description = "Customer ID", example = "customer123")
//            @RequestParam(defaultValue = "customer123") String customerId) {
//
//        long totalTransactions = transactionService.getTransactionCount(customerId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("customerId", customerId);
//        response.put("totalTransactions", totalTransactions);
//
//        return ResponseEntity.ok(response);
//    }

//    @DeleteMapping("/transactions")
//    @Operation(summary = "Clear test transactions", description = "Removes all transactions for a customer (for testing)")
//    public ResponseEntity<Map<String, Object>> clearTestTransactions(
//            @Parameter(description = "Customer ID", example = "customer123")
//            @RequestParam(defaultValue = "customer123") String customerId) {
//
//        log.info("Clearing test transactions for customer: {}", customerId);
//
//        int removedCount = transactionService.clearTransactionsForCustomer(customerId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Test transactions cleared successfully");
//        response.put("customerId", customerId);
//        response.put("removedCount", removedCount);
//
//        return ResponseEntity.ok(response);
//    }
//


//    @PostMapping("/kafka/send")
//    @Operation(summary = "Send transactions to Kafka", description = "Sends test transactions to Kafka topic for processing")
//    public ResponseEntity<Map<String, Object>> sendTransactionsToKafka(
//            @RequestParam(defaultValue = "customer123") String customerId,
//            @RequestParam(defaultValue = "3") int count) {
//
//        kafkaProducer.sendTestTransactions(customerId, count);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Transactions sent to Kafka successfully");
//        response.put("customerId", customerId);
//        response.put("count", count);
//        response.put("topic", "transactions");
//
//        return ResponseEntity.ok(response);
//    }


}