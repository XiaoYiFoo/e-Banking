package com.ebanking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot test to verify the application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the Spring application context loads successfully
    }
} 