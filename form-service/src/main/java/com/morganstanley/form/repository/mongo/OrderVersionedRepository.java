package com.morganstanley.form.repository.mongo;

import com.morganstanley.form.common.enums.OrderStatus;
import com.morganstanley.form.entity.mongo.OrderVersionedDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for OrderVersionedDocument.
 *
 * Provides CRUD operations and custom queries for versioned order data.
 * Uses Spring Data MongoDB query derivation and @Query annotations.
 */
@Repository
public interface OrderVersionedRepository extends MongoRepository<OrderVersionedDocument, String> {

    /**
     * Find the latest version of an order.
     * Only one document should have isLatestVersion = true for a given orderId.
     *
     * @param orderId the business key of the order
     * @return Optional containing the latest version, or empty if not found
     */
    Optional<OrderVersionedDocument> findByOrderIdAndIsLatestVersionTrue(String orderId);

    /**
     * Find a specific version of an order.
     *
     * @param orderId the business key of the order
     * @param orderVersionNumber the specific version number to retrieve
     * @return Optional containing the requested version, or empty if not found
     */
    Optional<OrderVersionedDocument> findByOrderIdAndOrderVersionNumber(
        String orderId,
        Integer orderVersionNumber
    );

    /**
     * Get all versions of an order, sorted by version number ascending.
     * Useful for displaying version history.
     *
     * @param orderId the business key of the order
     * @return List of all versions ordered by version number (oldest to newest)
     */
    List<OrderVersionedDocument> findByOrderIdOrderByOrderVersionNumberAsc(String orderId);

    /**
     * Get all versions of an order with a specific status.
     *
     * @param orderId the business key of the order
     * @param orderStatus the status to filter by (WIP or COMMITTED)
     * @return List of versions with the specified status
     */
    List<OrderVersionedDocument> findByOrderIdAndOrderStatus(
        String orderId,
        OrderStatus orderStatus
    );

    /**
     * Find all WIP versions for an order.
     * Used by purge service to identify versions to clean up.
     *
     * @param orderId the business key of the order
     * @return List of WIP versions
     */
    @Query("{ 'orderId': ?0, 'orderStatus': 'WIP' }")
    List<OrderVersionedDocument> findWipVersions(String orderId);

    /**
     * Find all COMMITTED versions for an order.
     * These are never deleted by purge jobs.
     *
     * @param orderId the business key of the order
     * @return List of committed versions
     */
    @Query("{ 'orderId': ?0, 'orderStatus': 'COMMITTED' }")
    List<OrderVersionedDocument> findCommittedVersions(String orderId);

    /**
     * Delete specific versions of an order by version numbers.
     * Used by purge service to clean up old WIP versions.
     *
     * @param orderId the business key of the order
     * @param versionNumbers list of version numbers to delete
     * @param orderStatus status filter (typically WIP)
     * @return count of deleted documents
     */
    @Query(value = "{ 'orderId': ?0, 'orderVersionNumber': { $in: ?1 }, 'orderStatus': ?2 }", delete = true)
    Long deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
        String orderId,
        List<Integer> versionNumbers,
        OrderStatus orderStatus
    );

    /**
     * Find all versions for an order.
     * Useful for service-layer operations that need full order-level traversal.
     *
     * @param orderId the business key of the order
     * @return List of all versions for the order
     */
    @Query("{ 'orderId': ?0 }")
    List<OrderVersionedDocument> findByOrderId(String orderId);

    /**
     * Count total versions for an order.
     *
     * @param orderId the business key of the order
     * @return total count of versions
     */
    Long countByOrderId(String orderId);

    /**
     * Count WIP versions for an order.
     *
     * @param orderId the business key of the order
     * @param orderStatus WIP status
     * @return count of WIP versions
     */
    Long countByOrderIdAndOrderStatus(String orderId, OrderStatus orderStatus);

    /**
     * Find orders modified within a date range.
     * Useful for reporting and analytics.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return List of orders modified within the range
     */
    List<OrderVersionedDocument> findByTimestampBetween(
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
