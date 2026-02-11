package com.morganstanley.form.controller;

import com.morganstanley.form.common.dto.CreateSchemaRequest;
import com.morganstanley.form.common.dto.SchemaResponse;
import com.morganstanley.form.service.SchemaManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST controller for form schema management.
 *
 * Provides endpoints for:
 * - Creating new form schemas
 * - Retrieving schemas by version
 * - Activating schema versions
 * - Deprecating old schemas
 *
 * Most operations require ADMIN role.
 *
 * Base path: /api/v1/schemas
 */
@RestController
@RequestMapping("/v1/schemas")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Schema Management", description = "APIs for managing form schemas (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class SchemaController {

    private final SchemaManagementService schemaManagementService;

    /**
     * Get the currently active form schema.
     *
     * This schema is used for all new orders.
     * Available to all authenticated users.
     *
     * @return ResponseEntity with active SchemaResponse
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active schema",
        description = "Retrieves the currently active form schema used for new orders."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active schema found"),
        @ApiResponse(responseCode = "404", description = "No active schema found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SchemaResponse> getActiveSchema() {
        log.info("GET /v1/schemas/active - Fetching active schema");

        SchemaResponse response = schemaManagementService.getActiveSchema();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific schema by version ID.
     *
     * @param formVersionId the schema version ID (e.g., v2.1.0)
     * @return ResponseEntity with SchemaResponse
     */
    @GetMapping("/{formVersionId}")
    @Operation(
        summary = "Get schema by version",
        description = "Retrieves a specific form schema by its version ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schema found"),
        @ApiResponse(responseCode = "404", description = "Schema not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SchemaResponse> getSchemaByVersionId(
            @Parameter(description = "Form version ID", example = "v2.1.0")
            @PathVariable String formVersionId) {

        log.info("GET /v1/schemas/{} - Fetching schema", formVersionId);

        SchemaResponse response = schemaManagementService.getSchemaByVersionId(formVersionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all form schemas.
     *
     * Returns all schemas ordered by creation date (newest first).
     * Admin only.
     *
     * @return ResponseEntity with List of SchemaResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all schemas",
        description = "Retrieves all form schemas ordered by creation date. Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schemas retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<List<SchemaResponse>> getAllSchemas() {
        log.info("GET /v1/schemas - Fetching all schemas");

        List<SchemaResponse> response = schemaManagementService.getAllSchemas();
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new form schema.
     *
     * The new schema is created in INACTIVE state.
     * Use the activate endpoint to make it active.
     * Admin only.
     *
     * @param request the schema creation request
     * @return ResponseEntity with created SchemaResponse and 201 status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create new schema",
        description = "Creates a new form schema version. Schema is inactive by default. Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Schema created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or version already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<SchemaResponse> createSchema(
            @Valid @RequestBody CreateSchemaRequest request) {

        log.info("POST /v1/schemas - Creating schema: {}", request.getFormVersionId());

        String userName = getCurrentUsername();
        SchemaResponse response = schemaManagementService.createNewSchema(request, userName);

        URI location = URI.create("/v1/schemas/" + response.getFormVersionId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Activate a schema version.
     *
     * Deactivates the currently active schema and activates the specified one.
     * All new orders will use this schema after activation.
     * Admin only.
     *
     * @param formVersionId the schema version to activate
     * @return ResponseEntity with activated SchemaResponse
     */
    @PutMapping("/{formVersionId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Activate schema",
        description = "Activates a schema version. Deactivates the currently active schema. Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schema activated successfully"),
        @ApiResponse(responseCode = "404", description = "Schema not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<SchemaResponse> activateSchema(
            @Parameter(description = "Form version ID to activate", example = "v2.1.0")
            @PathVariable String formVersionId) {

        log.info("PUT /v1/schemas/{}/activate - Activating schema", formVersionId);

        SchemaResponse response = schemaManagementService.activateSchema(formVersionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deprecate a schema version.
     *
     * Marks a schema as deprecated (soft delete).
     * Cannot deprecate the currently active schema.
     * Admin only.
     *
     * @param formVersionId the schema version to deprecate
     * @return ResponseEntity with 204 No Content
     */
    @DeleteMapping("/{formVersionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Deprecate schema",
        description = "Marks a schema as deprecated. Cannot deprecate active schema. Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Schema deprecated successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot deprecate active schema"),
        @ApiResponse(responseCode = "404", description = "Schema not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<Void> deprecateSchema(
            @Parameter(description = "Form version ID to deprecate", example = "v1.0.0")
            @PathVariable String formVersionId) {

        log.info("DELETE /v1/schemas/{} - Deprecating schema", formVersionId);

        schemaManagementService.deprecateSchema(formVersionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current username from security context.
     *
     * @return username or "admin"
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "admin";
    }
}
