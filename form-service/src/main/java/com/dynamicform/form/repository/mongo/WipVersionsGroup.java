package com.dynamicform.form.repository.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for aggregation query result.
 * Represents an order with its WIP version numbers.
 * Used by purge service to identify versions to clean up.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WipVersionsGroup {

    /**
     * Order ID with WIP versions.
     */
    private String orderId;

    /**
     * List of WIP version numbers for this order.
     * Example: [1, 2, 3]
     */
    private List<Integer> wipVersions;
}
