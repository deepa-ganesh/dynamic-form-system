package com.dynamicform.form.entity.mongo;

import com.dynamicform.form.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document storing complete order data with versioning.
 *
 * Each save operation creates a new immutable document with incremented version number.
 * This enables complete audit trail and point-in-time recovery.
 *
 * Collection: orders_versioned
 */
@Document(collection = "orders_versioned")
@CompoundIndexes({
    @CompoundIndex(
        name = "orderId_version_idx",
        def = "{'orderId': 1, 'orderVersionNumber': 1}",
        unique = true
    ),
    @CompoundIndex(
        name = "orderId_latest_idx",
        def = "{'orderId': 1, 'isLatestVersion': 1}"
    ),
    @CompoundIndex(
        name = "orderId_status_idx",
        def = "{'orderId': 1, 'orderStatus': 1}"
    )
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVersionedDocument {

    /**
     * MongoDB auto-generated document ID.
     * Format: ObjectId (24-character hex string)
     */
    @Id
    private String id;

    /**
     * Business key for the order.
     * Format: ORD-XXXXX (e.g., ORD-12345)
     * This remains constant across all versions of the same order.
     */
    @Indexed
    private String orderId;

    /**
     * Sequential version number starting from 1.
     * Combination of (orderId, orderVersionNumber) is unique.
     */
    private Integer orderVersionNumber;

    /**
     * Reference to the form schema version used when creating this order.
     * Format: vX.Y.Z (e.g., v2.1.0)
     * Enables rendering historical orders with their original schema.
     */
    @Indexed
    private String formVersionId;

    /**
     * Current status of this version.
     * WIP = Work In Progress (draft, auto-saved)
     * COMMITTED = Final submitted version
     */
    @Field("orderStatus")
    private OrderStatus orderStatus;

    /**
     * User who created this version.
     * Email or username from security context.
     */
    private String userName;

    /**
     * Timestamp when this version was created.
     * Auto-populated by Spring Data MongoDB.
     */
    @CreatedDate
    private LocalDateTime timestamp;

    /**
     * Flag indicating if this is the most recent version.
     * Only one version per orderId should have isLatestVersion = true.
     * Updated when newer version is created.
     */
    @Indexed
    private Boolean isLatestVersion;

    /**
     * Version number of the previous version (for linking).
     * Null for version 1.
     */
    private Integer previousVersionNumber;

    /**
     * Optional description of changes in this version.
     * Provided by user during save.
     */
    private String changeDescription;

    /**
     * Complete order data as dynamic JSON.
     * Structure defined by the formVersionId schema.
     *
     * Example structure:
     * {
     *   "deliveryLocations": ["Location A", "Location B"],
     *   "deliveryCompany": {
     *     "companyId": "DC-789",
     *     "name": "FastShip Logistics",
     *     "contact": "John Doe"
     *   },
     *   "items": [
     *     {
     *       "itemNumber": "ITEM-001",
     *       "quantity": 10,
     *       "price": 25.50
     *     }
     *   ]
     * }
     */
    @Field("data")
    private Map<String, Object> orderData;
}
