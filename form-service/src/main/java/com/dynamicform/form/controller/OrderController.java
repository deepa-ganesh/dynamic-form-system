package com.dynamicform.form.controller;

import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderSummaryResponse;
import com.dynamicform.form.common.dto.OrderVersionHistoryResponse;
import com.dynamicform.form.common.dto.OrderVersionResponse;
import com.dynamicform.form.common.dto.PromoteVersionRequest;
import com.dynamicform.form.common.dto.SchemaResponse;
import com.dynamicform.form.service.DataTransformationService;
import com.dynamicform.form.service.SchemaManagementService;
import com.dynamicform.form.service.VersionOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST controller for order management operations.
 *
 * Provides endpoints for:
 * - Creating new order versions (WIP and COMMITTED)
 * - Retrieving latest version of an order
 * - Retrieving specific version of an order
 * - Getting complete version history
 * - Getting committed versions only
 *
 * Base path: /api/v1/orders
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Order Management", description = "APIs for managing versioned orders")
public class OrderController {

    private final VersionOrchestrationService versionOrchestrationService;
    private final DataTransformationService dataTransformationService;
    private final SchemaManagementService schemaManagementService;

    /**
     * List latest order snapshot for all orders.
     *
     * @return list of latest order summaries
     */
    @GetMapping
    @Operation(
        summary = "List all orders",
        description = "Returns one latest snapshot per order with version counters."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order list returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OrderSummaryResponse>> listOrders() {
        log.info("GET /v1/orders - Fetching latest order snapshots");
        return ResponseEntity.ok(versionOrchestrationService.listLatestOrders());
    }

    /**
     * Create a new version of an order.
     *
     * If finalSave = false, creates a WIP (Work In Progress) version.
     * If finalSave = true, creates a COMMITTED version.
     *
     * Auto-increments version number and manages isLatestVersion flags.
     *
     * @param request the order creation request with validation
     * @return ResponseEntity with OrderVersionResponse and 201 Created status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create new order version",
        description = "Creates a new version of an order. Version number is auto-incremented. " +
                     "Use finalSave=false for drafts (WIP), finalSave=true for submission (COMMITTED)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order version created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "No active form schema found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderVersionResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("POST /v1/orders - Creating order: {}, finalSave: {}",
                 request.getOrderId(), request.isFinalSave());

        // Get username from security context
        String userName = getCurrentUsername();

        // Create new version
        OrderVersionResponse response = versionOrchestrationService.createNewVersion(request, userName);

        // Return 201 Created with Location header
        URI location = URI.create("/v1/orders/" + response.getOrderId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get the latest version of an order.
     *
     * Returns the version with isLatestVersion = true.
     * This could be either WIP or COMMITTED status.
     *
     * @param orderId the order ID (format: ORD-XXXXX)
     * @return ResponseEntity with OrderVersionResponse
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get latest order version",
        description = "Retrieves the most recent version of an order (the version with isLatestVersion=true)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderVersionResponse> getLatestVersion(
            @Parameter(description = "Order ID (e.g., ORD-12345)", example = "ORD-12345")
            @PathVariable String orderId) {

        log.info("GET /v1/orders/{} - Fetching latest version", orderId);

        OrderVersionResponse response = versionOrchestrationService.getLatestVersion(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific version of an order.
     *
     * @param orderId the order ID
     * @param versionNumber the specific version number to retrieve
     * @return ResponseEntity with OrderVersionResponse
     */
    @GetMapping("/{orderId}/versions/{versionNumber}")
    @Operation(
        summary = "Get specific order version",
        description = "Retrieves a specific version of an order by version number."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version found"),
        @ApiResponse(responseCode = "404", description = "Order or version not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderVersionResponse> getSpecificVersion(
            @Parameter(description = "Order ID", example = "ORD-12345")
            @PathVariable String orderId,
            @Parameter(description = "Version number", example = "1")
            @PathVariable @Min(1) Integer versionNumber) {

        log.info("GET /v1/orders/{}/versions/{} - Fetching specific version",
                 orderId, versionNumber);

        OrderVersionResponse response = versionOrchestrationService
            .getSpecificVersion(orderId, versionNumber);

        return ResponseEntity.ok(response);
    }

    /**
     * Promote a specific WIP version to a new committed version.
     *
     * This action creates a new immutable committed version and keeps
     * original WIP versions untouched.
     *
     * @param orderId order ID
     * @param versionNumber source WIP version number
     * @param request optional change description
     * @return newly created committed version
     */
    @PostMapping("/{orderId}/versions/{versionNumber}/promote")
    @Operation(
        summary = "Promote WIP version",
        description = "Promotes a WIP version by creating a new committed version from it."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "WIP version promoted"),
        @ApiResponse(responseCode = "400", description = "Version is not WIP or invalid request"),
        @ApiResponse(responseCode = "404", description = "Order/version not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderVersionResponse> promoteWipVersion(
            @Parameter(description = "Order ID", example = "ORD-12345")
            @PathVariable String orderId,
            @Parameter(description = "WIP version number to promote", example = "3")
            @PathVariable @Min(1) Integer versionNumber,
            @RequestBody(required = false) PromoteVersionRequest request) {

        log.info("POST /v1/orders/{}/versions/{}/promote - Promoting WIP version", orderId, versionNumber);
        String userName = getCurrentUsername();
        String changeDescription = request != null ? request.getChangeDescription() : null;

        OrderVersionResponse response = versionOrchestrationService.promoteWipVersion(
            orderId,
            versionNumber,
            userName,
            changeDescription
        );

        URI location = URI.create("/v1/orders/" + orderId + "/versions/" + response.getOrderVersionNumber());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get complete version history for an order.
     *
     * Returns all versions (both WIP and COMMITTED) with summary information.
     * Versions are ordered by version number ascending (oldest to newest).
     *
     * @param orderId the order ID
     * @return ResponseEntity with OrderVersionHistoryResponse containing all versions
     */
    @GetMapping("/{orderId}/versions")
    @Operation(
        summary = "Get order version history",
        description = "Retrieves complete version history for an order, including both WIP and COMMITTED versions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderVersionHistoryResponse> getVersionHistory(
            @Parameter(description = "Order ID", example = "ORD-12345")
            @PathVariable String orderId) {

        log.info("GET /v1/orders/{}/versions - Fetching version history", orderId);

        OrderVersionHistoryResponse response = versionOrchestrationService.getVersionHistory(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get only committed versions of an order.
     *
     * Returns only versions with status = COMMITTED.
     * Useful for displaying official submission history.
     *
     * @param orderId the order ID
     * @return ResponseEntity with List of committed versions
     */
    @GetMapping("/{orderId}/committed-versions")
    @Operation(
        summary = "Get committed order versions",
        description = "Retrieves only the committed (final) versions of an order, excluding WIP drafts."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Committed versions retrieved"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OrderVersionResponse>> getCommittedVersions(
            @Parameter(description = "Order ID", example = "ORD-12345")
            @PathVariable String orderId) {

        log.info("GET /v1/orders/{}/committed-versions - Fetching committed versions", orderId);

        List<OrderVersionResponse> response = versionOrchestrationService.getCommittedVersions(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Prefill order data from dimensional tables using configured field mappings.
     *
     * This endpoint helps populate dynamic JSON form payloads from existing SQL tables.
     * If formVersionId is omitted, the currently active schema version is used.
     *
     * @param sourceTable source dimensional table name
     * @param sourceKeyColumn source table key column
     * @param sourceKeyValue source key value
     * @param formVersionId optional form schema version
     * @return transformed JSON payload map
     */
    @GetMapping("/prefill")
    @Operation(
        summary = "Prefill from dimensional tables",
        description = "Transforms dimensional-table data into JSON payload format using field mappings."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prefill data generated"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Active schema or mapping not found")
    })
    public ResponseEntity<Map<String, Object>> prefillFromDimensional(
            @Parameter(description = "Source dimensional table", example = "delivery_companies")
            @RequestParam String sourceTable,
            @Parameter(description = "Source key column", example = "company_id")
            @RequestParam String sourceKeyColumn,
            @Parameter(description = "Source key value", example = "DC-001")
            @RequestParam String sourceKeyValue,
            @Parameter(description = "Optional form version ID. If missing, active schema is used.", example = "v1.0.0")
            @RequestParam(required = false) String formVersionId) {

        String effectiveFormVersionId = formVersionId;
        if (effectiveFormVersionId == null || effectiveFormVersionId.isBlank()) {
            SchemaResponse activeSchema = schemaManagementService.getActiveSchema();
            effectiveFormVersionId = activeSchema.getFormVersionId();
        }

        Map<String, Object> transformedData = dataTransformationService.transformDimensionalToJSON(
            effectiveFormVersionId,
            sourceTable,
            sourceKeyColumn,
            sourceKeyValue
        );

        return ResponseEntity.ok(transformedData);
    }

    /**
     * List available prefill mappings for a schema version.
     *
     * UI can use this endpoint to show valid source tables and key-column suggestions
     * instead of hardcoded prefill defaults.
     *
     * @param formVersionId optional form version ID. Active schema is used when omitted.
     * @return grouped mapping metadata by source table
     */
    @GetMapping("/prefill/mappings")
    @Operation(
        summary = "List prefill mapping options",
        description = "Returns available active field mappings grouped by source table for the selected schema version."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mapping options returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Active schema not found")
    })
    public ResponseEntity<Map<String, Object>> getPrefillMappings(
            @Parameter(description = "Optional form version ID. If missing, active schema is used.", example = "v1.0.0")
            @RequestParam(required = false) String formVersionId) {

        String effectiveFormVersionId = formVersionId;
        if (effectiveFormVersionId == null || effectiveFormVersionId.isBlank()) {
            SchemaResponse activeSchema = schemaManagementService.getActiveSchema();
            effectiveFormVersionId = activeSchema.getFormVersionId();
        }

        Map<String, Object> mappingOptions = dataTransformationService.listPrefillMappings(effectiveFormVersionId);
        return ResponseEntity.ok(mappingOptions);
    }

    /**
     * Get current username from security context.
     * Falls back to "system" if no authentication present.
     *
     * @return username or "system"
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
