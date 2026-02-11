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

import java.time.LocalDateTime;

/**
 * Lightweight index collection for fast order version queries.
 *
 * This collection mirrors orders_versioned but excludes the large 'data' field,
 * enabling fast queries for version lists, status checks, and purge operations.
 *
 * Collection: order_version_index
 */
@Document(collection = "order_version_index")
@CompoundIndexes({
    @CompoundIndex(
        name = "idx_orderId_version",
        def = "{'orderId': 1, 'orderVersionNumber': 1}",
        unique = true
    ),
    @CompoundIndex(
        name = "idx_orderId_latest",
        def = "{'orderId': 1, 'isLatestVersion': 1}"
    ),
    @CompoundIndex(
        name = "idx_status_timestamp",
        def = "{'orderStatus': 1, 'timestamp': 1}"
    )
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVersionIndex {

    /**
     * MongoDB auto-generated document ID.
     */
    @Id
    private String id;

    /**
     * Business key for the order.
     * Format: ORD-XXXXX
     */
    @Indexed
    private String orderId;

    /**
     * Sequential version number.
     */
    private Integer orderVersionNumber;

    /**
     * Form schema version reference.
     */
    @Indexed
    private String formVersionId;

    /**
     * Status: WIP or COMMITTED
     */
    private OrderStatus orderStatus;

    /**
     * User who created this version.
     */
    private String userName;

    /**
     * Creation timestamp.
     */
    @CreatedDate
    private LocalDateTime timestamp;

    /**
     * Latest version flag.
     */
    @Indexed
    private Boolean isLatestVersion;

    /**
     * Previous version link.
     */
    private Integer previousVersionNumber;

    /**
     * Change description.
     */
    private String changeDescription;

    /**
     * Size of the document in bytes (for monitoring).
     * Calculated before saving to index.
     */
    private Integer documentSize;
}
