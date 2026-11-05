package com.fintech.ledger.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger UI Configuration for the Fintech Core Ledger Engine.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fintech Core Ledger & Instant Payment Engine API")
                        .version("1.0.0")
                        .description("High-performance Double-Entry Bookkeeping Ledger and Instant Payment Settlement Engine. " +
                                     "Architected with Clean Architecture (DDD), deterministic anti-deadlock lock ordering (UUID ASC), " +
                                     "idempotency signatures (SHA-256), and sub-10ms transactional execution.")
                        .contact(new Contact()
                                .name("Mauricio G. Oliveira")
                                .url("https://github.com/Mauricio-G-Oliveira/fintech-core-ledger"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://fintech-ledger.internal").description("Production Gateway (Virtual)")
                ));
    }
}
