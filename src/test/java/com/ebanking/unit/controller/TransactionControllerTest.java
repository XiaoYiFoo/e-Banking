package com.ebanking.unit.controller;

import com.ebanking.controller.TransactionController;
import com.ebanking.domain.Transaction;
import com.ebanking.dto.AddTransactionResponse;
import com.ebanking.dto.TransactionRequest;
import com.ebanking.dto.TransactionResponse;
import com.ebanking.service.KafkaTransactionProducer;
import com.ebanking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private KafkaTransactionProducer kafkaProducer;

    @InjectMocks
    private TransactionController controller;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(authentication.getName()).thenReturn("P-0123456789");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getTransactions_ReturnsResponse() {
        TransactionResponse mockResponse = TransactionResponse.builder()
                .transactions(List.of())
                .totalCredit(BigDecimal.ZERO)
                .totalDebit(BigDecimal.ZERO)
                .baseCurrency("USD")
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(0)
                .first(true)
                .last(true)
                .build();

        when(transactionService.getTransactions(eq("P-0123456789"), any(TransactionRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<TransactionResponse> response = controller.getTransactions(
                7, 2024, 0, 20, "USD"
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("USD", response.getBody().getBaseCurrency());
        verify(transactionService).getTransactions(eq("P-0123456789"), any(TransactionRequest.class));
    }

    @Test
    void addTransaction_Success() {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(BigDecimal.valueOf(100))
                .currency("USD")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.now())
                .description("desc")
                .customerId("P-0123456789")
                .build();

        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenReturn(null); // Simulate success

        ResponseEntity<AddTransactionResponse> response = controller.addTransaction(
                authentication,
                BigDecimal.valueOf(100),
                "USD",
                "CH93-0000-0000-0000-0000-0",
                "desc",
                "2024-07-15"
        );

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("success", response.getBody().getStatus());
        verify(kafkaProducer).sendTransactionSync(any(Transaction.class));
    }

    @Test
    void addTransaction_KafkaFailure_ReturnsError() {
        when(kafkaProducer.sendTransactionSync(any(Transaction.class)))
                .thenThrow(new RuntimeException("Kafka error"));

        ResponseEntity<AddTransactionResponse> response = controller.addTransaction(
                authentication,
                BigDecimal.valueOf(100),
                "USD",
                "CH93-0000-0000-0000-0000-0",
                "desc",
                "2024-07-15"
        );

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("failed", response.getBody().getStatus());
    }
}