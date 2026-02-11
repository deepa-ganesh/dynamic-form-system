package com.morganstanley.form.batch.config;

import com.morganstanley.form.batch.job.PurgeTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for purge job.
 *
 * Defines the batch job and step for WIP version cleanup.
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class PurgeJobConfig {

    private final PurgeTasklet purgeTasklet;

    /**
     * Define the purge job.
     *
     * @param jobRepository Spring Batch job repository
     * @param purgeStep the purge step
     * @return configured Job
     */
    @Bean(name = "purgeJob")
    public Job purgeJob(JobRepository jobRepository, Step purgeStep) {
        log.info("Configuring purge job");

        return new JobBuilder("purgeWipVersionsJob", jobRepository)
            .start(purgeStep)
            .build();
    }

    /**
     * Define the purge step.
     *
     * @param jobRepository Spring Batch job repository
     * @param transactionManager transaction manager
     * @return configured Step
     */
    @Bean
    public Step purgeStep(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager) {
        log.info("Configuring purge step");

        return new StepBuilder("purgeWipVersionsStep", jobRepository)
            .tasklet(purgeTasklet, transactionManager)
            .build();
    }
}
