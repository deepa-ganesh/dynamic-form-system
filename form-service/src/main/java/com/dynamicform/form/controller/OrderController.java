package com.dynamicform.form.controller;

import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderVersionHistoryResponse;
import com.dynamicform.form.common.dto.OrderVersionResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

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
