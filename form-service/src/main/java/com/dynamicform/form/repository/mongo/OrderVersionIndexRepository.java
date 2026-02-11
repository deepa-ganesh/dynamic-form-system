package com.dynamicform.form.repository.mongo;

import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.entity.mongo.OrderVersionIndex;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for OrderVersionIndex.
 *
 * Lightweight index repository for fast queries without loading full order data.
 * Used by purge service and version history queries.
 */
@Repository
public interface OrderVersionIndexRepository extends MongoRepository<OrderVersionIndex, String> {

    /**
     * Find the latest version index for an order.
     *
     * @param orderId the business key of the order
     * @return Optional containing the latest version index
     */
    Optional<OrderVersionIndex> findByOrderIdAndIsLatestVersionTrue(String orderId);

    /**
     * Find the index entry with the highest version number for an order.
     * This query supports append-only versioning where latest is derived
     * from version number ordering rather than mutable flags.
     *
     * @param orderId the business key of the order
     * @return Optional containing the highest version index
     */
    Optional<OrderVersionIndex> findTopByOrderIdOrderByOrderVersionNumberDesc(String orderId);

    /**
     * Find a specific version index.
     *
     * @param orderId the business key of the order
     * @param orderVersionNumber the version number
     * @return Optional containing the requested version index
     */
    Optional<OrderVersionIndex> findByOrderIdAndOrderVersionNumber(
        String orderId,
        Integer orderVersionNumber
    );

    /**
     * Get all version indexes for an order, sorted by version number.
     *
     * @param orderId the business key of the order
     * @return List of version indexes ordered by version number
     */
    List<OrderVersionIndex> findByOrderIdOrderByOrderVersionNumberAsc(String orderId);

    /**
     * Find all WIP version indexes for an order.
     *
     * @param orderId the business key of the order
     * @return List of WIP version indexes
     */
    @Query("{ 'orderId': ?0, 'orderStatus': 'WIP' }")
    List<OrderVersionIndex> findWipVersionIndexes(String orderId);

    /**
     * Find all orders that have WIP versions.
     * Used by purge service to identify orders needing cleanup.
     *
     * Returns grouped data with orderId and list of WIP version numbers.
     *
     * @return List of WipVersionsGroup objects
     */
    @Aggregation(pipeline = {
        "{ $match: { 'orderStatus': 'WIP' } }",
        "{ $group: { " +
        "    _id: '$orderId', " +
        "    orderId: { $first: '$orderId' }, " +
        "    wipVersions: { $push: '$orderVersionNumber' } " +
        "} }",
        "{ $project: { " +
        "    _id: 0, " +
        "    orderId: 1, " +
        "    wipVersions: 1 " +
        "} }"
    })
    List<WipVersionsGroup> findOrdersWithWipVersions();

    /**
     * Delete specific version indexes.
     *
     * @param orderId the business key of the order
     * @param versionNumbers list of version numbers to delete
     * @param orderStatus status filter
     * @return count of deleted documents
     */
    @Query(value = "{ 'orderId': ?0, 'orderVersionNumber': { $in: ?1 }, 'orderStatus': ?2 }", delete = true)
    Long deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
        String orderId,
        List<Integer> versionNumbers,
        OrderStatus orderStatus
    );

    /**
     * Count total versions for an order.
     *
     * @param orderId the business key of the order
     * @return count of versions
     */
    Long countByOrderId(String orderId);

    /**
     * Count versions by status.
     *
     * @param orderId the business key of the order
     * @param orderStatus status to count
     * @return count of versions with specified status
     */
    Long countByOrderIdAndOrderStatus(String orderId, OrderStatus orderStatus);

    /**
     * List latest order snapshot per order ID with version counts.
     *
     * @return latest order summaries sorted by timestamp descending
     */
    @Aggregation(pipeline = {
        "{ $sort: { 'orderId': 1, 'orderVersionNumber': -1, 'timestamp': -1 } }",
        "{ $group: { " +
        "    _id: '$orderId', " +
        "    orderId: { $first: '$orderId' }, " +
        "    latestVersionNumber: { $first: '$orderVersionNumber' }, " +
        "    formVersionId: { $first: '$formVersionId' }, " +
        "    orderStatus: { $first: '$orderStatus' }, " +
        "    userName: { $first: '$userName' }, " +
        "    timestamp: { $first: '$timestamp' }, " +
        "    changeDescription: { $first: '$changeDescription' }, " +
        "    totalVersions: { $sum: 1 }, " +
        "    committedVersions: { $sum: { $cond: [ { $eq: ['$orderStatus', 'COMMITTED'] }, 1, 0 ] } }, " +
        "    wipVersions: { $sum: { $cond: [ { $eq: ['$orderStatus', 'WIP'] }, 1, 0 ] } } " +
        "} }",
        "{ $project: { _id: 0, orderId: 1, latestVersionNumber: 1, formVersionId: 1, orderStatus: 1, userName: 1, timestamp: 1, changeDescription: 1, totalVersions: 1, committedVersions: 1, wipVersions: 1 } }",
        "{ $sort: { 'timestamp': -1 } }"
    })
    List<OrderLatestSummaryProjection> findLatestOrderSummaries();
}
