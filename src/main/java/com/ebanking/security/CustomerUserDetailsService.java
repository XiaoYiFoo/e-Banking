package com.ebanking.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for customer authentication.
 * 
 * This service loads customer details for JWT authentication.
 * In a real implementation, this would typically query a database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String customerId) throws UsernameNotFoundException {
        log.debug("Loading user details for customer: {}", customerId);
        
        // In a real implementation, this would query a database or external service
        // to validate the customer exists and get their details
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new UsernameNotFoundException("Customer ID cannot be empty");
        }
        
        // For this implementation, we assume all valid customer IDs are authenticated
        // In production, you would validate against a customer database
        return User.builder()
                .username(customerId)
                .password("") // No password needed for JWT authentication
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
} 