package com.ebanking.unit.service;

import com.ebanking.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exchange Rate Service Unit Tests")
class ExchangeRateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateApiUrl", "https://api.exchangerate-api.com/v4/latest");
        ReflectionTestUtils.setField(exchangeRateService, "timeout", 5000);
    }

    @Test
    @DisplayName("convertToBaseCurrency - Same currency should return original amount")
    void convertToBaseCurrency_SameCurrency_ReturnsOriginalAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, currency, currency, LocalDate.now());

        // Assert
        assertEquals(amount, result);
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("convertToBaseCurrency - Null amount should return zero")
    void convertToBaseCurrency_NullAmount_ReturnsZero() {
        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(null, "USD", "GBP", LocalDate.now());

        // Assert
        assertEquals(BigDecimal.ZERO, result);
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("convertToBaseCurrency - Null fromCurrency should return original amount")
    void convertToBaseCurrency_NullFromCurrency_ReturnsOriginalAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, null, "GBP", LocalDate.now());

        // Assert
        assertEquals(amount, result);
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("convertToBaseCurrency - Null toCurrency should return original amount")
    void convertToBaseCurrency_NullToCurrency_ReturnsOriginalAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", null, LocalDate.now());

        // Assert
        assertEquals(amount, result);
    }

    @Test
    @DisplayName("convertToBaseCurrency - Valid conversion should return converted amount")
    void convertToBaseCurrency_ValidConversion_ReturnsConvertedAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("79.00"), result);
        verify(restTemplate).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("convertToBaseCurrency - Should use cached rate for same request")
    void convertToBaseCurrency_ShouldUseCachedRate() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act - First call should fetch from API
        BigDecimal result1 = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Second call should use cache
        BigDecimal result2 = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("79.00"), result1);
        assertEquals(new BigDecimal("79.00"), result2);

        // Verify API was called only once
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("convertToBaseCurrency - Different date should use different cache key")
    void convertToBaseCurrency_DifferentDate_UsesDifferentCacheKey() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);

        Map<String, Object> apiResponse1 = new HashMap<>();
        Map<String, Object> rates1 = new HashMap<>();
        rates1.put("GBP", 0.79);
        apiResponse1.put("rates", rates1);

        Map<String, Object> apiResponse2 = new HashMap<>();
        Map<String, Object> rates2 = new HashMap<>();
        rates2.put("GBP", 0.80);
        apiResponse2.put("rates", rates2);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse1)
                .thenReturn(apiResponse2);

        // Act
        BigDecimal result1 = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date1);
        BigDecimal result2 = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date2);

        // Assert
        assertEquals(new BigDecimal("79.00"), result1);
        assertEquals(new BigDecimal("80.00"), result2);

        // Verify API was called twice (once for each date)
        verify(restTemplate, times(2)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("convertToBaseCurrency - Should round to 2 decimal places")
    void convertToBaseCurrency_ShouldRoundToTwoDecimalPlaces() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.123456); // Rate with many decimal places
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("12.35"), result); // Rounded to 2 decimal places
    }



    @Test
    @DisplayName("convertToBaseCurrency - Negative amount should return negative converted amount")
    void convertToBaseCurrency_NegativeAmount_ReturnsNegativeConvertedAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("-100.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("-79.00"), result);
    }

    @Test
    @DisplayName("clearCache - Should clear the rate cache")
    void clearCache_ShouldClearCache() {
        // Arrange - First populate cache
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Act
        exchangeRateService.clearCache();

        // Act again - should call API again since cache is cleared
        exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert - API should be called twice (once before clear, once after)
        verify(restTemplate, times(2)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("convertToBaseCurrency - Case insensitive currency comparison")
    void convertToBaseCurrency_CaseInsensitiveCurrency_Works() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert - Should work with different cases
        BigDecimal result1 = exchangeRateService.convertToBaseCurrency(amount, "USD", "usd", LocalDate.now());
        BigDecimal result2 = exchangeRateService.convertToBaseCurrency(amount, "usd", "USD", LocalDate.now());
        BigDecimal result3 = exchangeRateService.convertToBaseCurrency(amount, "Usd", "uSd", LocalDate.now());

        assertEquals(amount, result1);
        assertEquals(amount, result2);
        assertEquals(amount, result3);

        // Verify no API calls were made for same currency
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("convertToBaseCurrency - Large amount should handle correctly")
    void convertToBaseCurrency_LargeAmount_HandlesCorrectly() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000000.00");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("790000.00"), result);
    }

    @Test
    @DisplayName("convertToBaseCurrency - Very small amount should handle correctly")
    void convertToBaseCurrency_VerySmallAmount_HandlesCorrectly() {
        // Arrange
        BigDecimal amount = new BigDecimal("0.01");
        LocalDate date = LocalDate.now();

        Map<String, Object> apiResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("GBP", 0.79);
        apiResponse.put("rates", rates);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        // Act
        BigDecimal result = exchangeRateService.convertToBaseCurrency(amount, "USD", "GBP", date);

        // Assert
        assertEquals(new BigDecimal("0.01"), result); // Rounded to 2 decimal places
    }
}