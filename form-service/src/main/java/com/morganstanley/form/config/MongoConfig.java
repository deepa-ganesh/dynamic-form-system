package com.morganstanley.form.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration.
 *
 * Enables MongoDB auditing features:
 * - @CreatedDate - Auto-populate timestamp on insert
 * - @LastModifiedDate - Auto-populate timestamp on update
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Configuration is done through annotations
    // Spring Boot auto-configures MongoTemplate from application.yml
}
