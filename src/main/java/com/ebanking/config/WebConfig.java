package com.ebanking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Web configuration for HTTP client operations.
 * 
 * Provides RestTemplate bean for external API calls.
 */
@Configuration
public class WebConfig {

    @Value("${app.exchange-rate.api.timeout:5000}")
    private int timeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        
        return new RestTemplate(factory);
    }
} 