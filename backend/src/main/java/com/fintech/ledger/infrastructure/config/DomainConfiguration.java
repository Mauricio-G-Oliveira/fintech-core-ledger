package com.fintech.ledger.infrastructure.config;

import com.fintech.ledger.domain.service.DoubleEntryRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing beans for framework-agnostic Domain Services.
 * Preserves Clean Architecture principles by keeping domain classes pure.
 */
@Configuration
public class DomainConfiguration {

    @Bean
    public DoubleEntryRuleEngine doubleEntryRuleEngine() {
        return new DoubleEntryRuleEngine();
    }
}
