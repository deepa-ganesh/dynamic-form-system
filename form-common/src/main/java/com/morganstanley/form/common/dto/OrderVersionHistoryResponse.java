package com.morganstanley.form.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing complete version history for an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVersionHistoryResponse {

    private String orderId;
    private Integer totalVersions;
    private Integer committedVersions;
    private Integer wipVersions;
    private List<VersionSummaryDTO> versions;
}
