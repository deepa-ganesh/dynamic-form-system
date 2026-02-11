package com.morganstanley.form.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Order ID is required")
    @Pattern(regexp = "^ORD-[0-9]{5}$", message = "Order ID must be in format ORD-XXXXX")
    private String orderId;

    @NotEmpty(message = "At least one delivery location is required")
    @Size(min = 1, max = 10, message = "Between 1 and 10 delivery locations allowed")
    private List<@NotBlank(message = "Delivery location cannot be blank") String> deliveryLocations;

    @Valid
    @NotNull(message = "Order data is required")
    private Map<String, Object> data;

    private boolean finalSave;

    private String changeDescription;
}
