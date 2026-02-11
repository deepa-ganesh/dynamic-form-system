package com.dynamicform.form.repository.mongo;

import com.dynamicform.form.entity.mongo.PurgeAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for PurgeAuditLog.
 *
 * Stores audit trail of purge operations.
 */
@Repository
public interface PurgeAuditLogRepository extends MongoRepository<PurgeAuditLog, String> {

    /**
     * Find a purge log by purge ID.
     *
     * @param purgeId unique purge execution ID
     * @return Optional containing the purge log
     */
    Optional<PurgeAuditLog> findByPurgeId(String purgeId);

    /**
     * Find all purge logs within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return List of purge logs in the date range
     */
    List<PurgeAuditLog> findByPurgeStartTimeBetween(
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Find the most recent purge logs.
     *
     * @return List of recent purge logs, ordered by start time descending
     */
    List<PurgeAuditLog> findTop10ByOrderByPurgeStartTimeDesc();

    /**
     * Find purge logs by status.
     *
     * @param purgeStatus status (SUCCESS, FAILED, PARTIAL)
     * @return List of purge logs with specified status
     */
    List<PurgeAuditLog> findByPurgeStatus(String purgeStatus);
}
