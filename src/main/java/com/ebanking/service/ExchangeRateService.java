
package com.ebanking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    
    @Value("${app.exchange-rate.api.base-url}")
    private String exchangeRateApiUrl;
    
    @Value("${app.exchange-rate.api.timeout:5000}")
    private int timeout;
    
    private final ConcurrentHashMap<String, BigDecimal> rateCache = new ConcurrentHashMap<>();

    public BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            log.warn("Invalid parameters for currency conversion: amount={}, from={}, to={}", 
                    amount, fromCurrency, toCurrency);
            return amount != null ? amount : BigDecimal.ZERO;
        }
        
        // If same currency, return original amount
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }
        
        try {
            BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency, date);
            return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            log.error("Error converting currency from {} to {}: {}", fromCurrency, toCurrency, e.getMessage());
            // Return original amount in case of error (don't fail the entire request)
            return amount;
        }
    }

    private BigDecimal getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) {
        String cacheKey = String.format("%s_%s_%s", fromCurrency, toCurrency, date);
        
        // Check cache first
        BigDecimal cachedRate = rateCache.get(cacheKey);
        if (cachedRate != null) {
            log.debug("Using cached exchange rate for {} to {}: {}", fromCurrency, toCurrency, cachedRate);
            return cachedRate;
        }
        
        // Fetch from external API
        BigDecimal rate = fetchExchangeRateFromApi(fromCurrency, toCurrency, date);
        
        // Cache the rate
        if (rate != null) {
            rateCache.put(cacheKey, rate);
        }
        
        return rate != null ? rate : BigDecimal.ONE; // Default to 1:1 if API fails
    }

    private BigDecimal fetchExchangeRateFromApi(String fromCurrency, String toCurrency, LocalDate date) {
        try {
            // For demo purposes, use mock rates if API is unavailable
            if (isApiUnavailable()) {
                log.warn("Exchange rate API unavailable, using mock rates for {} to {}", fromCurrency, toCurrency);
                return getMockExchangeRate(fromCurrency, toCurrency);
            }
            
            String url = String.format("%s/%s", exchangeRateApiUrl, fromCurrency);
            log.debug("Fetching exchange rate from API: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                
                if (rates.containsKey(toCurrency)) {
                    Object rateObj = rates.get(toCurrency);
                    if (rateObj instanceof Number) {
                        return new BigDecimal(rateObj.toString());
                    }
                }
            }
            
            log.warn("Could not extract exchange rate from API response for {} to {}", fromCurrency, toCurrency);
            return getMockExchangeRate(fromCurrency, toCurrency);
            
        } catch (ResourceAccessException e) {
            log.warn("Network error accessing exchange rate API: {}", e.getMessage());
            return getMockExchangeRate(fromCurrency, toCurrency);
        } catch (HttpClientErrorException e) {
            log.warn("HTTP error accessing exchange rate API: {} - {}", e.getStatusCode(), e.getMessage());
            return getMockExchangeRate(fromCurrency, toCurrency);
        } catch (Exception e) {
            log.error("Error fetching exchange rate from API for {} to {}: {}", 
                     fromCurrency, toCurrency, e.getMessage());
            return getMockExchangeRate(fromCurrency, toCurrency);
        }
    }

    /**
     * Check if the external API is available.
     */
    private boolean isApiUnavailable() {
        // For demo purposes, you can add a simple health check here
        // In production, you might want to implement a circuit breaker pattern
        return false; // Assume API is available by default
    }

    /**
     * Get mock exchange rates for demo purposes.
     */
    private BigDecimal getMockExchangeRate(String fromCurrency, String toCurrency) {
        // Simple mock rates for demo
        if ("USD".equalsIgnoreCase(fromCurrency) && "GBP".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("0.79");
        } else if ("GBP".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("1.27");
        } else if ("EUR".equalsIgnoreCase(fromCurrency) && "GBP".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("0.86");
        } else if ("GBP".equalsIgnoreCase(fromCurrency) && "EUR".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("1.16");
        } else if ("CHF".equalsIgnoreCase(fromCurrency) && "GBP".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("0.89");
        } else if ("GBP".equalsIgnoreCase(fromCurrency) && "CHF".equalsIgnoreCase(toCurrency)) {
            return new BigDecimal("1.12");
        }
        
        // Default 1:1 rate for unknown currency pairs
        return BigDecimal.ONE;
    }

    public void clearCache() {
        rateCache.clear();
        log.debug("Exchange rate cache cleared");
    }
}