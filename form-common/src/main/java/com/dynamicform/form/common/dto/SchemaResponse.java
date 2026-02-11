package com.dynamicform.form.common.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO containing form schema details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaResponse {

    private Long id;
    private String formVersionId;
    private String formName;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime deprecatedDate;
    private String createdBy;
    private JsonNode fieldDefinitions;
}
