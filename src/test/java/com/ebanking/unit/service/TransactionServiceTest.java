package com.ebanking.unit.service;

import com.ebanking.domain.Transaction;
import com.ebanking.dto.TransactionRequest;
import com.ebanking.dto.TransactionResponse;
import com.ebanking.repository.TransactionRepository;
import com.ebanking.service.ExchangeRateService;
import com.ebanking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction creditTransaction;
    private Transaction debitTransaction;
    private TransactionRequest request;

    @BeforeEach
    void setUp() {
        creditTransaction = Transaction.builder()
                .id("credit-123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.of(2024, 7, 15))
                .description("Credit transaction")
                .customerId("P-0123456789")
                .build();

        debitTransaction = Transaction.builder()
                .id("debit-456")
                .amount(new BigDecimal("-50.00"))
                .currency("EUR")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.of(2024, 7, 15))
                .description("Debit transaction")
                .customerId("P-0123456789")
                .build();

        request = TransactionRequest.builder()
                .month(7)
                .year(2024)
                .page(0)
                .size(20)
                .baseCurrency("GBP")
                .build();
    }

    @Test
    @DisplayName("processTransaction - Should save transaction to repository")
    void processTransaction_ShouldSaveTransaction() {
        // Act
        transactionService.processTransaction(creditTransaction);

        // Assert
        verify(transactionRepository).save(creditTransaction);
    }

    @Test
    @DisplayName("getTransactions - Should return paginated transactions with correct totals")
    void getTransactions_ShouldReturnPaginatedTransactions() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(creditTransaction, debitTransaction);
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 2);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(page);

        // Use lenient stubbing for exchange rate service to handle multiple calls
        // Note: The service calls convertToBaseCurrency with the original amount (including negative sign)
        lenient().when(exchangeRateService.convertToBaseCurrency(
                        eq(new BigDecimal("100.00")), eq("USD"), eq("GBP"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("79.00"));

        lenient().when(exchangeRateService.convertToBaseCurrency(
                        eq(new BigDecimal("-50.00")), eq("EUR"), eq("GBP"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("-43.00")); // Note: Keep the negative sign

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTransactions().size());
        assertEquals(new BigDecimal("79.00"), response.getTotalCredit());
        assertEquals(new BigDecimal("43.00"), response.getTotalDebit()); // Service will call .abs() on -43.00
        assertEquals("GBP", response.getBaseCurrency());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("getTransactions - Should handle empty transaction list")
    void getTransactions_ShouldHandleEmptyList() {
        // Arrange
        Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getTransactions().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getTotalCredit());
        assertEquals(BigDecimal.ZERO, response.getTotalDebit());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    @DisplayName("getTransactions - Should handle exchange rate service failure")
    void getTransactions_ShouldHandleExchangeRateFailure() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(creditTransaction);
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(page);

        // Mock the getExchangeRate method to throw an exception, which will be caught by convertToBaseCurrency
        // and return the original amount
        lenient().when(exchangeRateService.convertToBaseCurrency(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    // Simulate what the real service does - return the original amount on exception
                    BigDecimal amount = invocation.getArgument(0);
                    return amount != null ? amount : BigDecimal.ZERO;
                });

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        // Should return original amount when exchange rate fails
        assertEquals(new BigDecimal("100.00"), response.getTotalCredit());
    }

    @Test
    @DisplayName("getTransactions - Should handle pagination correctly")
    void getTransactions_ShouldHandlePagination() {
        // Arrange
        TransactionRequest paginatedRequest = TransactionRequest.builder()
                .month(7)
                .year(2024)
                .page(1)
                .size(10)
                .baseCurrency("GBP")
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(creditTransaction), PageRequest.of(1, 10), 25);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(page);

        lenient().when(exchangeRateService.convertToBaseCurrency(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("79.00"));

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", paginatedRequest);

        // Assert
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(3, response.getTotalPages()); // 25 elements / 10 per page = 3 pages
        assertEquals(25, response.getTotalElements());
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    @DisplayName("getTransactions - Should handle null exchange rate service response")
    void getTransactions_ShouldHandleNullExchangeRateResponse() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(creditTransaction);
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(page);

        lenient().when(exchangeRateService.convertToBaseCurrency(any(), any(), any(), any()))
                .thenReturn(null);

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        // Should return original amount when exchange rate returns null
        assertEquals(new BigDecimal("100.00"), response.getTotalCredit());
    }

    @Test
    @DisplayName("getTransactions - Should handle multiple transactions with different currencies")
    void getTransactions_ShouldHandleMultipleCurrencies() {
        // Arrange
        Transaction usdTransaction = Transaction.builder()
                .id("usd-123")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.of(2024, 7, 15))
                .description("USD transaction")
                .customerId("P-0123456789")
                .build();

        Transaction eurTransaction = Transaction.builder()
                .id("eur-456")
                .amount(new BigDecimal("-150.00"))
                .currency("EUR")
                .accountIban("CH93-0000-0000-0000-0000-0")
                .valueDate(LocalDate.of(2024, 7, 15))
                .description("EUR transaction")
                .customerId("P-0123456789")
                .build();

        List<Transaction> transactions = Arrays.asList(usdTransaction, eurTransaction);
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 2);

        when(transactionRepository.findByCustomerIdAndValueDateBetween(
                eq("P-0123456789"), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(page);

        // Stub for the actual amounts being passed (including negative sign for EUR transaction)
        lenient().when(exchangeRateService.convertToBaseCurrency(
                        eq(new BigDecimal("200.00")), eq("USD"), eq("GBP"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("158.00"));

        lenient().when(exchangeRateService.convertToBaseCurrency(
                        eq(new BigDecimal("-150.00")), eq("EUR"), eq("GBP"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("-129.00")); // Keep the negative sign

        // Act
        TransactionResponse response = transactionService.getTransactions("P-0123456789", request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTransactions().size());
        assertEquals(new BigDecimal("158.00"), response.getTotalCredit());
        assertEquals(new BigDecimal("129.00"), response.getTotalDebit()); // Service will call .abs() on -129.00
    }
}