package com.careflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Dedicated JPA configuration.
 * Kept separate from @SpringBootApplication to avoid breaking sliced WebMvc tests (§11, §58).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
