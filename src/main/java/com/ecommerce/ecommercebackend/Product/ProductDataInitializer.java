package com.ecommerce.ecommercebackend.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A one-time data initializer to fix the Hibernate `@Version` uninitialized issue.
 * Hibernate 6+ requires that existing entities with an ID have a non-null version.
 * This runs on startup and updates any `null` version values to `0`.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking for products with uninitialized version value...");
        
        try {
            // Update only rows where version is NULL to avoid unnecessary writes/locking
            int updatedRows = jdbcTemplate.update("UPDATE products SET version = 0 WHERE version IS NULL");
            
            if (updatedRows > 0) {
                log.info("Successfully initialized version for {} products.", updatedRows);
            } else {
                log.info("All products have already been initialized.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize product version: {}", e.getMessage());
        }
    }
}
