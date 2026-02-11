package com.dynamicform.form.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for PostgreSQL.
 *
 * Enables:
 * - JPA auditing (@CreatedDate, @LastModifiedDate)
 * - JPA repositories in specified package
 * - Transaction management
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.dynamicform.form.repository.postgres")
@EnableTransactionManagement
public class JpaConfig {
    // Configuration is done through annotations
    // Spring Boot auto-configures EntityManager from application.yml
}
