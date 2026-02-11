package com.dynamicform.form.common.dto;

import com.dynamicform.form.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for listing latest order snapshots.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

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
