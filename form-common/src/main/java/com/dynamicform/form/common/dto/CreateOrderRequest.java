package com.dynamicform.form.common.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new order version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String orderId;

    private List<String> deliveryLocations;

    @Valid
    private Map<String, Object> data;

    private boolean finalSave;

    private String changeDescription;
}
