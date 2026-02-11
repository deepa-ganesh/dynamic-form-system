package com.dynamicform.form.common.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for field mapping configuration details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingResponse {

    private Long id;
    private String formVersionId;
    private String sourceTable;
    private String sourceColumn;
    private String targetFieldPath;
    private String dataType;
    private String transformationFunction;
    private Boolean isRequired;
    private String defaultValue;
    private Integer processingOrder;
    private JsonNode metadata;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private String createdBy;
}
