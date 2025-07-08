package com.ebanking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for the e-Banking Transaction Service.
 * 
 * This microservice provides REST API endpoints for retrieving paginated transaction lists
 * with exchange rate conversion for authenticated users.
 */
@SpringBootApplication
@EnableKafka
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
} 