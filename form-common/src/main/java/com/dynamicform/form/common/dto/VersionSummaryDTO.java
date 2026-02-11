package com.dynamicform.form.common.dto;

import com.dynamicform.form.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for version history lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionSummaryDTO {

    private String orderId;
    private Integer orderVersionNumber;
    private String formVersionId;
    private OrderStatus orderStatus;
    private String userName;
    private LocalDateTime timestamp;
    private Boolean isLatestVersion;
    private String changeDescription;
}
