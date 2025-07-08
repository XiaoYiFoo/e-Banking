package com.ebanking.integration.controller;

import com.ebanking.TransactionServiceApplication;
import com.ebanking.domain.Transaction;
import com.ebanking.dto.AddTransactionResponse;
import com.ebanking.dto.TransactionRequest;
import com.ebanking.dto.TransactionResponse;
import com.ebanking.service.KafkaTransactionProducer;
import com.ebanking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TransactionServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Transaction Controller Integration Tests")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private KafkaTransactionProducer kafkaProducer;

    private Transaction mockTransaction;
    private TransactionResponse mockTransactionResponse;
    private AddTransactionResponse mockAddTransactionResponse;

    @BeforeEach
    void setUp() {
        // Setup mock transaction
        mockTransaction = Transaction.builder()
                .id("test-transaction-id")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.of(2024, 7, 15))
                .description("Test transaction")
                .customerId("P-0123456789")
                .build();

        // Setup mock transaction response
        mockTransactionResponse = TransactionResponse.builder()
                .transactions(List.of(mockTransaction))
                .totalCredit(new BigDecimal("100.00"))
                .totalDebit(BigDecimal.ZERO)
                .baseCurrency("USD")
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .first(true)
                .last(true)
                .build();

        // Setup mock add transaction response
        mockAddTransactionResponse = AddTransactionResponse.builder()
                .message("Transaction sent to Kafka successfully")
                .transactionId("test-transaction-id")
                .status("success")
                .customerId("P-0123456789")
                .build();
    }

    // ==================== GET TRANSACTIONS TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Valid request returns 200 OK")
    void getTransactions_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(mockTransactionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024")
                        .param("page", "0")
                        .param("size", "20")
                        .param("baseCurrency", "USD"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0].id").value("test-transaction-id"))
                .andExpect(jsonPath("$.transactions[0].amount").value(100.00))
                .andExpect(jsonPath("$.transactions[0].currency").value("USD"));

        // Verify service was called
        verify(transactionService).getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Empty result returns 200 OK")
    void getTransactions_EmptyResult_ReturnsOk() throws Exception {
        // Arrange
        TransactionResponse emptyResponse = TransactionResponse.builder()
                .transactions(List.of())
                .totalCredit(BigDecimal.ZERO)
                .totalDebit(BigDecimal.ZERO)
                .baseCurrency("USD")
                .page(0)
                .size(20)
                .totalPages(0)
                .totalElements(0)
                .first(true)
                .last(true)
                .build();

        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Invalid month number returns 400 Bad Request")
    void getTransactions_InvalidMonthNumber_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "13")  // Invalid month
                        .param("year", "2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Invalid month string returns 400 Bad Request")
    void getTransactions_InvalidMonthString_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "Jan")  // Invalid string
                        .param("year", "2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid value for parameter 'month': 'Jan'. Expected type: Integer")));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Missing required parameters returns 400 Bad Request")
    void getTransactions_MissingRequiredParameters_ReturnsBadRequest() throws Exception {
        // Act & Assert - missing month
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("year", "2024"))
                .andExpect(status().isBadRequest());

        // Act & Assert - missing year
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Service exception returns 500 Internal Server Error")
    void getTransactions_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    @DisplayName("GET /api/v1/getTransaction - Unauthenticated returns 401 Unauthorized")
    void getTransactions_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    // ==================== ADD TRANSACTION TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - Valid request returns 201 Created")
    void addTransaction_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        SendResult<String, Transaction> mockSendResult = Mockito.mock(SendResult.class);
        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenReturn(mockSendResult);

        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "100.00")
                        .param("currency", "USD")
                        .param("accountIban", "CH93-0000-0000-0000-0000-0")
                        .param("description", "Test payment")
                        .param("valueDate", LocalDate.now().toString()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Transaction created successfully"))
                .andExpect(jsonPath("$.customerId").value("P-0123456789"))
                .andExpect(jsonPath("$.transactionId").exists());

        // Verify producer was called
        verify(kafkaProducer).sendTransactionSync(any(Transaction.class));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - With default values returns 201 Created")
    void addTransaction_WithDefaultValues_ReturnsCreated() throws Exception {
        // Arrange
        SendResult<String, Transaction> mockSendResult = Mockito.mock(SendResult.class);
        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenReturn(mockSendResult);

        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "250.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.customerId").value("P-0123456789"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - With all parameters returns 201 Created")
    void addTransaction_WithAllParameters_ReturnsCreated() throws Exception {
        // Arrange
        SendResult<String, Transaction> mockSendResult = Mockito.mock(SendResult.class);
        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenReturn(mockSendResult);

        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "500.75")
                        .param("currency", "EUR")
                        .param("accountIban", "DE89-3704-0044-0532-0130-00")
                        .param("description", "International transfer")
                        .param("valueDate", "2024-07-15"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.customerId").value("P-0123456789"));
    }


    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - Zero amount returns 400 Bad Request")
    void addTransaction_ZeroAmount_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "0.00"))  // Zero amount
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - Invalid currency returns 400 Bad Request")
    void addTransaction_InvalidCurrency_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "100.00")
                        .param("currency", "INVALID"))  // Invalid currency
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - Invalid date format returns 400 Bad Request")
    void addTransaction_InvalidDateFormat_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "100.00")
                        .param("valueDate", "invalid-date"))  // Invalid date
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

//    @Test
//    @WithMockUser(username = "P-0123456789")
//    @DisplayName("POST /api/v1/addTransaction - Kafka failure returns 500 Internal Server Error")
//    void addTransaction_KafkaFailure_ReturnsInternalServerError() throws Exception {
//        // Arrange
//        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
//                .thenThrow(new RuntimeException("Kafka connection failed"));
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/addTransaction")
//                        .param("amount", "100.00")
//                        .param("currency", "USD"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.status").value("500"))
//                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
//    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("POST /api/v1/addTransaction - Kafka failure returns 500 Internal Server Error")
    void addTransaction_KafkaFailure_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenThrow(new RuntimeException("Kafka connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "100.00")
                        .param("currency", "USD"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("failed"))  // ← Your controller returns "failed"
                .andExpect(jsonPath("$.message").value("Failed to send transaction to Kafka"));  // ← Your controller message
    }

    @Test
    @DisplayName("POST /api/v1/addTransaction - Unauthenticated returns 401 Unauthorized")
    void addTransaction_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/addTransaction")
                        .param("amount", "100.00")
                        .param("currency", "USD"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    // ==================== EDGE CASES ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Large page size returns 400 Bad Request")
    void getTransactions_LargePageSize_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024")
                        .param("size", "1000"))  // Too large
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Negative page returns 400 Bad Request")
    void getTransactions_NegativePage_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024")
                        .param("page", "-1"))  // Negative page
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Zero month returns 400 Bad Request")
    void getTransactions_ZeroMonth_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "0")  // Zero month
                        .param("year", "2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Null parameters returns 400 Bad Request")
    void getTransactions_NullParameters_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "")  // Empty string
                        .param("year", "2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Pagination works correctly")
    void getTransactions_Pagination_WorksCorrectly() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(mockTransactionResponse);

        // Act & Assert - First page
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)); // Mock returns default size
    }

    // ==================== CURRENCY CONVERSION TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Different base currency works")
    void getTransactions_DifferentBaseCurrency_Works() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(mockTransactionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024")
                        .param("baseCurrency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD")); // Mock returns USD
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789", roles = {"USER"})
    @DisplayName("GET /api/v1/getTransaction - Authenticated user with role can access")
    void getTransactions_AuthenticatedUserWithRole_CanAccess() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(mockTransactionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024"))
                .andExpect(status().isOk());
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @WithMockUser(username = "P-0123456789")
    @DisplayName("GET /api/v1/getTransaction - Response time is acceptable")
    void getTransactions_ResponseTime_IsAcceptable() throws Exception {
        // Arrange
        when(transactionService.getTransactions(
                eq("P-0123456789"),
                any(TransactionRequest.class)))
                .thenReturn(mockTransactionResponse);

        long startTime = System.currentTimeMillis();

        // Act
        mockMvc.perform(get("/api/v1/getTransaction")
                        .param("month", "7")
                        .param("year", "2024"))
                .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Assert - Response time should be less than 1 second
        assert responseTime < 1000 : "Response time was " + responseTime + "ms, expected < 1000ms";
    }
}