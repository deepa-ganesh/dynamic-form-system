package com.morganstanley.form.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * Scheduler for the purge job.
 *
 * Executes the purge job daily at midnight (00:00 AM).
 * Uses cron expression for scheduling.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PurgeJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job purgeJob;

    /**
     * Schedule purge job to run daily at midnight.
     *
     * Cron expression: "0 0 0 * * ?" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 0 (midnight)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: ? (any)
     *
     * For testing, you can change to:
     * - Every minute: "0 * * * * ?"
     * - Every 5 minutes: "0 every-5-minutes * * * ?"
     * - Every hour: "0 0 * * * ?"
     */
    @Scheduled(cron = "0 0 0 * * ?")  // Daily at midnight
    public void runPurgeJob() {
        log.info("====================================================");
        log.info("SCHEDULED PURGE JOB TRIGGERED");
        log.info("Time: {}", LocalDateTime.now());
        log.info("====================================================");

        try {
            // Create unique job parameters (required for each execution)
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("startTime", System.currentTimeMillis())
                .addString("executionTime", LocalDateTime.now().toString())
                .toJobParameters();

            // Launch the job
            jobLauncher.run(purgeJob, jobParameters);

            log.info("Purge job execution completed successfully");

        } catch (Exception e) {
            log.error("Failed to execute purge job", e);
        }
    }

    /**
     * Manual trigger method for testing.
     * Can be called via REST endpoint or admin interface.
     *
     * @throws Exception if job execution fails
     */
    public void triggerManualPurge() throws Exception {
        log.info("MANUAL PURGE TRIGGERED");

        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("startTime", System.currentTimeMillis())
            .addString("executionTime", LocalDateTime.now().toString())
            .addString("triggerType", "MANUAL")
            .toJobParameters();

        jobLauncher.run(purgeJob, jobParameters);
    }
}
