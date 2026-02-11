package com.dynamicform.form.repository.mongo;

import com.dynamicform.form.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregation projection for latest order snapshot per order ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLatestSummaryProjection {

    private String orderId;
    private Integer latestVersionNumber;
    private String formVersionId;
    private OrderStatus orderStatus;
    private String userName;
    private LocalDateTime timestamp;
    private String changeDescription;
    private Integer totalVersions;
    private Integer committedVersions;
    private Integer wipVersions;
}
