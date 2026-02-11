package com.dynamicform.form.common.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating field mappings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingRequest {

    @NotBlank(message = "Source table is required")
    private String sourceTable;

    @NotBlank(message = "Source column is required")
    private String sourceColumn;

    @NotBlank(message = "Target field path is required")
    private String targetFieldPath;

    @NotBlank(message = "Data type is required")
    private String dataType;

    private String transformationFunction;

    @Builder.Default
    private Boolean isRequired = false;

    private String defaultValue;

    @Min(value = 0, message = "Processing order must be greater than or equal to 0")
    @Builder.Default
    private Integer processingOrder = 100;

    private JsonNode metadata;

    @Builder.Default
    private Boolean isActive = true;
}
