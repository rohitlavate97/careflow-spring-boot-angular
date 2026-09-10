package com.careflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * CareFlow — Enterprise Hospital & Healthcare Operations Platform
 * Main Spring Boot Application Entrypoint.
 */
@SpringBootApplication
@EnableTransactionManagement
public class CareFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareFlowApplication.class, args);
    }
}
