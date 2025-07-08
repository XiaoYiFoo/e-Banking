package com.ebanking.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple test configuration for e-Banking application.
 *
 * This configuration provides:
 * - Test-specific configurations
 * - No embedded Kafka (uses mocks instead)
 */
@TestConfiguration
@ActiveProfiles("test")
public class TestConfig {
    // Test configuration - mocks will be defined in individual test classes
}