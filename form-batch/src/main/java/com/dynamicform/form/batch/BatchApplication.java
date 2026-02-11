package com.dynamicform.form.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main application class for batch jobs.
 *
 * This module runs scheduled batch jobs:
 * - Daily purge of WIP versions (midnight)
 *
 * Can be run standalone or integrated with main application.
 */
@SpringBootApplication(scanBasePackages = {
    "com.dynamicform.form.batch",
    "com.dynamicform.form.common"
})
@EnableMongoRepositories(basePackages = "com.dynamicform.form.repository.mongo")
@EntityScan(basePackages = "com.dynamicform.form.entity.mongo")
@Slf4j
public class BatchApplication {

    public static void main(String[] args) {
        log.info("====================================================");
        log.info("DYNAMIC FORM SYSTEM - BATCH MODULE");
        log.info("Starting Batch Job Application...");
        log.info("====================================================");

        SpringApplication.run(BatchApplication.class, args);

        log.info("Batch application started successfully");
        log.info("Purge job scheduled to run daily at midnight (00:00)");
    }
}
