package com.morganstanley.form.repository.mongo;

import com.morganstanley.form.common.enums.OrderStatus;
import com.morganstanley.form.entity.mongo.OrderVersionIndex;
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
}
