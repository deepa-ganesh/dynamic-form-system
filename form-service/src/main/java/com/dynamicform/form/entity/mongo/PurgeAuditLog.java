package com.dynamicform.form.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit log for purge operations.
 *
 * Records details of each purge job execution, including which versions
 * were deleted and which were retained.
 *
 * Collection: purge_audit_log
 */
@Document(collection = "purge_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurgeAuditLog {

    /**
     * MongoDB auto-generated document ID.
     */
    @Id
    private String id;

    /**
     * Unique identifier for this purge execution.
     * Format: PURGE-YYYYMMDD-HHMMSS
     * Example: PURGE-20260211-000005
     */
    @Indexed
    private String purgeId;

    /**
     * When the purge job started.
     */
    @Indexed
    private LocalDateTime purgeStartTime;

    /**
     * When the purge job completed.
     */
    private LocalDateTime purgeEndTime;

    /**
     * Duration in milliseconds.
     */
    private Long durationMs;

    /**
     * Status of purge execution.
     * Values: SUCCESS, FAILED, PARTIAL
     */
    private String purgeStatus;

    /**
     * Total number of unique orders processed.
     */
    private Integer totalOrdersProcessed;

    /**
     * Total number of versions deleted.
     */
    private Integer totalVersionsDeleted;

    /**
     * Total number of versions retained.
     */
    private Integer totalVersionsRetained;

    /**
     * List of order IDs that were processed.
     * Useful for debugging and verification.
     */
    private List<String> processedOrderIds;

    /**
     * Detailed breakdown per order.
     * List of PurgeDetail objects.
     */
    private List<PurgeDetail> purgeDetails;

    /**
     * Error message if purge failed.
     */
    private String errorMessage;

    /**
     * Nested class for per-order purge details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurgeDetail {

        /**
         * Order ID that was processed.
         */
        private String orderId;

        /**
         * Version numbers that were deleted.
         * Example: [1, 2, 3]
         */
        private List<Integer> deletedVersions;

        /**
         * Version number that was retained (latest WIP).
         * Example: 5
         */
        private Integer retainedWipVersion;

        /**
         * Count of committed versions (never deleted).
         */
        private Integer committedVersionsCount;
    }
}
