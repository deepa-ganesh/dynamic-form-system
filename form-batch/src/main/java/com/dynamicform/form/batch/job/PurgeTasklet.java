package com.dynamicform.form.batch.job;

import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.entity.mongo.PurgeAuditLog;
import com.dynamicform.form.repository.mongo.OrderVersionIndexRepository;
import com.dynamicform.form.repository.mongo.OrderVersionedRepository;
import com.dynamicform.form.repository.mongo.PurgeAuditLogRepository;
import com.dynamicform.form.repository.mongo.WipVersionsGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring Batch Tasklet for purging old WIP versions.
 *
 * Executes daily at midnight to clean up work-in-progress versions,
 * keeping only the latest WIP version per order.
 *
 * Algorithm:
 * 1. Query all orders that have WIP versions
 * 2. For each order:
 *    a. Get all WIP version numbers
 *    b. Sort descending (highest first)
 *    c. Keep the highest version
 *    d. Delete all others
 * 3. Create audit log of purge execution
 *
 * This is a CRITICAL job for storage management and data hygiene.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeTasklet implements Tasklet {

    private final OrderVersionedRepository orderVersionedRepository;
    private final OrderVersionIndexRepository orderVersionIndexRepository;
    private final PurgeAuditLogRepository purgeAuditLogRepository;

    /**
     * Execute the purge tasklet.
     *
     * @param contribution step contribution
     * @param chunkContext chunk context
     * @return RepeatStatus.FINISHED when complete
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        LocalDateTime purgeStartTime = LocalDateTime.now();
        String purgeId = generatePurgeId(purgeStartTime);

        log.info("====================================================");
        log.info("PURGE JOB STARTED: {}", purgeId);
        log.info("Start Time: {}", purgeStartTime);
        log.info("====================================================");

        // Statistics counters
        int totalOrdersProcessed = 0;
        int totalVersionsDeleted = 0;
        int totalVersionsRetained = 0;
        List<String> processedOrderIds = new ArrayList<>();
        List<PurgeAuditLog.PurgeDetail> purgeDetails = new ArrayList<>();
        String purgeStatus = "SUCCESS";
        String errorMessage = null;

        try {
            // Step 1: Get all orders with WIP versions using aggregation
            log.info("Querying orders with WIP versions...");
            List<WipVersionsGroup> ordersWithWip = orderVersionIndexRepository.findOrdersWithWipVersions();
            log.info("Found {} orders with WIP versions", ordersWithWip.size());

            // Step 2: Process each order
            for (WipVersionsGroup wipGroup : ordersWithWip) {
                try {
                    PurgeAuditLog.PurgeDetail detail = processOrder(wipGroup);
                    purgeDetails.add(detail);
                    processedOrderIds.add(wipGroup.getOrderId());

                    totalOrdersProcessed++;
                    totalVersionsDeleted += detail.getDeletedVersions().size();
                    if (detail.getRetainedWipVersion() != null) {
                        totalVersionsRetained++;
                    }

                } catch (Exception e) {
                    log.error("Error processing order {}: {}", wipGroup.getOrderId(), e.getMessage(), e);
                    // Continue with next order even if this one fails
                    purgeStatus = "PARTIAL";
                }
            }

        } catch (Exception e) {
            log.error("CRITICAL ERROR during purge execution", e);
            purgeStatus = "FAILED";
            errorMessage = e.getMessage();
        }

        // Step 3: Calculate duration and create audit log
        LocalDateTime purgeEndTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(purgeStartTime, purgeEndTime).toMillis();

        PurgeAuditLog auditLog = PurgeAuditLog.builder()
            .purgeId(purgeId)
            .purgeStartTime(purgeStartTime)
            .purgeEndTime(purgeEndTime)
            .durationMs(durationMs)
            .purgeStatus(purgeStatus)
            .totalOrdersProcessed(totalOrdersProcessed)
            .totalVersionsDeleted(totalVersionsDeleted)
            .totalVersionsRetained(totalVersionsRetained)
            .processedOrderIds(processedOrderIds)
            .purgeDetails(purgeDetails)
            .errorMessage(errorMessage)
            .build();

        purgeAuditLogRepository.save(auditLog);

        // Step 4: Log summary
        log.info("====================================================");
        log.info("PURGE JOB COMPLETED: {}", purgeId);
        log.info("Status: {}", purgeStatus);
        log.info("Duration: {} ms ({} seconds)", durationMs, durationMs / 1000);
        log.info("Orders Processed: {}", totalOrdersProcessed);
        log.info("Versions Deleted: {}", totalVersionsDeleted);
        log.info("Versions Retained: {}", totalVersionsRetained);
        if (errorMessage != null) {
            log.error("Error Message: {}", errorMessage);
        }
        log.info("====================================================");

        return RepeatStatus.FINISHED;
    }

    /**
     * Process a single order's WIP versions.
     *
     * Algorithm:
     * 1. Get WIP version list
     * 2. Sort descending (highest first)
     * 3. Retain highest version
     * 4. Delete all others
     *
     * @param wipGroup the order with WIP versions
     * @return PurgeDetail with results
     */
    private PurgeAuditLog.PurgeDetail processOrder(WipVersionsGroup wipGroup) {
        String orderId = wipGroup.getOrderId();
        List<Integer> wipVersions = new ArrayList<>(wipGroup.getWipVersions());

        log.debug("Processing order: {} with {} WIP versions: {}",
                 orderId, wipVersions.size(), wipVersions);

        if (wipVersions.isEmpty()) {
            log.warn("No WIP versions found for order: {}", orderId);
            return PurgeAuditLog.PurgeDetail.builder()
                .orderId(orderId)
                .deletedVersions(Collections.emptyList())
                .retainedWipVersion(null)
                .committedVersionsCount(0)
                .build();
        }

        // Sort descending to get highest version first
        wipVersions.sort(Collections.reverseOrder());

        // Highest version is retained
        Integer retainedVersion = wipVersions.get(0);

        // All others are deleted
        List<Integer> versionsToDelete = wipVersions.subList(1, wipVersions.size());

        if (!versionsToDelete.isEmpty()) {
            log.info("Order {}: Retaining version {}, deleting versions: {}",
                    orderId, retainedVersion, versionsToDelete);

            // Delete from main collection
            Long deletedFromMain = orderVersionedRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                orderId, versionsToDelete, OrderStatus.WIP
            );

            // Delete from index collection
            Long deletedFromIndex = orderVersionIndexRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                orderId, versionsToDelete, OrderStatus.WIP
            );

            log.debug("Order {}: Deleted {} documents from main collection, {} from index",
                     orderId, deletedFromMain, deletedFromIndex);
        } else {
            log.debug("Order {}: Only one WIP version exists ({}), nothing to delete",
                     orderId, retainedVersion);
        }

        // Count committed versions (never deleted)
        Long committedCount = orderVersionIndexRepository.countByOrderIdAndOrderStatus(
            orderId, OrderStatus.COMMITTED
        );

        return PurgeAuditLog.PurgeDetail.builder()
            .orderId(orderId)
            .deletedVersions(versionsToDelete)
            .retainedWipVersion(retainedVersion)
            .committedVersionsCount(committedCount.intValue())
            .build();
    }

    /**
     * Generate unique purge ID.
     * Format: PURGE-YYYYMMDD-HHMMSS
     * Example: PURGE-20260211-000005
     *
     * @param timestamp the purge start time
     * @return purge ID string
     */
    private String generatePurgeId(LocalDateTime timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "PURGE-" + timestamp.format(formatter);
    }
}
