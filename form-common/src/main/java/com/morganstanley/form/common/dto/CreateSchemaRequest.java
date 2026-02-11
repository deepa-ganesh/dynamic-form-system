package com.morganstanley.form.common.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new form schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemaRequest {

    @NotBlank(message = "Form version ID is required")
    @Pattern(regexp = "^v[0-9]+\\.[0-9]+\\.[0-9]+$", message = "Form version ID must be in format vX.Y.Z")
    private String formVersionId;

    @NotBlank(message = "Form name is required")
    private String formName;

    private String description;

    @NotNull(message = "Field definitions are required")
    private JsonNode fieldDefinitions;
}
