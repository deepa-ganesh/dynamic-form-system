package com.morganstanley.form.common.dto;

import com.morganstanley.form.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO containing order version details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVersionResponse {

    private String orderId;
    private Integer orderVersionNumber;
    private String formVersionId;
    private OrderStatus orderStatus;
    private String userName;
    private LocalDateTime timestamp;
    private Boolean isLatestVersion;
    private Integer previousVersionNumber;
    private String changeDescription;
    private Map<String, Object> data;
}
