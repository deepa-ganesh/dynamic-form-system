package com.dynamicform.form.service;

import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderVersionHistoryResponse;
import com.dynamicform.form.common.dto.OrderVersionResponse;
import com.dynamicform.form.common.dto.VersionSummaryDTO;
import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.common.exception.OrderNotFoundException;
import com.dynamicform.form.common.exception.SchemaNotFoundException;
import com.dynamicform.form.entity.mongo.OrderVersionIndex;
import com.dynamicform.form.entity.mongo.OrderVersionedDocument;
import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import com.dynamicform.form.repository.mongo.OrderVersionIndexRepository;
import com.dynamicform.form.repository.mongo.OrderVersionedRepository;
import com.dynamicform.form.repository.postgres.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core service for version orchestration.
 *
 * Manages the complete lifecycle of order versions:
 * - Creating new versions with automatic version numbering
 * - Managing isLatestVersion flags
 * - Handling WIP vs COMMITTED status
 * - Maintaining version history
 *
 * This is the MOST CRITICAL service in the application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionOrchestrationService {

    private final OrderVersionedRepository orderVersionedRepository;
    private final OrderVersionIndexRepository orderVersionIndexRepository;
    private final FormSchemaRepository formSchemaRepository;

    /**
     * Create a new version of an order.
     *
     * Algorithm:
     * 1. Get active form schema
     * 2. Determine next version number (query latest version + 1)
     * 3. Create new OrderVersionedDocument
     * 4. Set status (WIP or COMMITTED based on finalSave flag)
     * 5. Save to orders_versioned collection
     * 6. Update isLatestVersion flag on previous version
     * 7. Create corresponding OrderVersionIndex entry
     * 8. Return response DTO
     *
     * @param request the order creation request
     * @param userName the user creating this version (from security context)
     * @return OrderVersionResponse with new version details
     * @throws SchemaNotFoundException if no active schema exists
     */
    @Transactional
    public OrderVersionResponse createNewVersion(CreateOrderRequest request, String userName) {
        log.info("Creating new version for orderId: {}, finalSave: {}",
                 request.getOrderId(), request.isFinalSave());

        // Step 1: Get active form schema
        FormSchemaEntity activeSchema = formSchemaRepository.findByIsActiveTrue()
            .orElseThrow(() -> new SchemaNotFoundException("No active form schema found"));

        String formVersionId = activeSchema.getFormVersionId();
        log.debug("Using form schema version: {}", formVersionId);

        // Step 2: Determine next version number
        Optional<OrderVersionIndex> latestVersionIndex =
            orderVersionIndexRepository.findByOrderIdAndIsLatestVersionTrue(request.getOrderId());

        int newVersionNumber;
        Integer previousVersionNumber = null;

        if (latestVersionIndex.isPresent()) {
            // Existing order - increment version
            newVersionNumber = latestVersionIndex.get().getOrderVersionNumber() + 1;
            previousVersionNumber = latestVersionIndex.get().getOrderVersionNumber();
            log.debug("Existing order. Creating version {} (previous: {})",
                     newVersionNumber, previousVersionNumber);
        } else {
            // New order - start at version 1
            newVersionNumber = 1;
            log.debug("New order. Creating version 1");
        }

        // Step 3: Determine status based on finalSave flag
        OrderStatus orderStatus = request.isFinalSave() ? OrderStatus.COMMITTED : OrderStatus.WIP;

        // Step 4: Build new document
        OrderVersionedDocument newDocument = OrderVersionedDocument.builder()
            .orderId(request.getOrderId())
            .orderVersionNumber(newVersionNumber)
            .formVersionId(formVersionId)
            .orderStatus(orderStatus)
            .userName(userName)
            .timestamp(LocalDateTime.now())
            .isLatestVersion(true)  // This is now the latest
            .previousVersionNumber(previousVersionNumber)
            .changeDescription(request.getChangeDescription())
            .orderData(request.getData())
            .build();

        // Step 5: Save to MongoDB
        OrderVersionedDocument savedDocument = orderVersionedRepository.save(newDocument);
        log.info("Saved version {} for orderId: {} with status: {}",
                 newVersionNumber, request.getOrderId(), orderStatus);

        // Step 6: Update isLatestVersion flag on previous version
        if (latestVersionIndex.isPresent()) {
            updatePreviousVersionFlag(request.getOrderId(), previousVersionNumber);
        }

        // Step 7: Create index entry
        createVersionIndex(savedDocument);

        // Step 8: Return response DTO
        return mapToResponse(savedDocument);
    }

    /**
     * Update isLatestVersion flag on the previous version.
     * Sets isLatestVersion = false for the old latest version.
     *
     * @param orderId the order ID
     * @param previousVersionNumber the version to update
     */
    private void updatePreviousVersionFlag(String orderId, Integer previousVersionNumber) {
        log.debug("Updating isLatestVersion flag for orderId: {}, version: {}",
                 orderId, previousVersionNumber);

        Optional<OrderVersionedDocument> previousDoc =
            orderVersionedRepository.findByOrderIdAndOrderVersionNumber(orderId, previousVersionNumber);

        if (previousDoc.isPresent()) {
            OrderVersionedDocument doc = previousDoc.get();
            doc.setIsLatestVersion(false);
            orderVersionedRepository.save(doc);
            log.debug("Updated previous version {} to isLatestVersion = false", previousVersionNumber);
        }

        // Also update the index
        Optional<OrderVersionIndex> previousIndex =
            orderVersionIndexRepository.findByOrderIdAndOrderVersionNumber(orderId, previousVersionNumber);

        if (previousIndex.isPresent()) {
            OrderVersionIndex index = previousIndex.get();
            index.setIsLatestVersion(false);
            orderVersionIndexRepository.save(index);
        }
    }

    /**
     * Create a lightweight index entry for fast queries.
     *
     * @param document the full order document
     */
    private void createVersionIndex(OrderVersionedDocument document) {
        int documentSize = calculateDocumentSize(document);

        OrderVersionIndex index = OrderVersionIndex.builder()
            .orderId(document.getOrderId())
            .orderVersionNumber(document.getOrderVersionNumber())
            .formVersionId(document.getFormVersionId())
            .orderStatus(document.getOrderStatus())
            .userName(document.getUserName())
            .timestamp(document.getTimestamp())
            .isLatestVersion(document.getIsLatestVersion())
            .previousVersionNumber(document.getPreviousVersionNumber())
            .changeDescription(document.getChangeDescription())
            .documentSize(documentSize)
            .build();

        orderVersionIndexRepository.save(index);
        log.debug("Created version index for orderId: {}, version: {}",
                 document.getOrderId(), document.getOrderVersionNumber());
    }

    /**
     * Calculate approximate document size in bytes.
     * Used for monitoring and analytics.
     *
     * @param document the order document
     * @return estimated size in bytes
     */
    private int calculateDocumentSize(OrderVersionedDocument document) {
        // Rough estimation:
        // - Base document fields: ~500 bytes
        // - Data field: JSON string length * 2 (UTF-16)
        int baseSize = 500;
        int dataSize = document.getOrderData() != null ?
                      document.getOrderData().toString().length() * 2 : 0;
        return baseSize + dataSize;
    }

    /**
     * Get the latest version of an order.
     *
     * @param orderId the order ID
     * @return OrderVersionResponse
     * @throws OrderNotFoundException if order doesn't exist
     */
    public OrderVersionResponse getLatestVersion(String orderId) {
        log.debug("Fetching latest version for orderId: {}", orderId);

        OrderVersionedDocument document = orderVersionedRepository
            .findByOrderIdAndIsLatestVersionTrue(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapToResponse(document);
    }

    /**
     * Get a specific version of an order.
     *
     * @param orderId the order ID
     * @param versionNumber the version number to retrieve
     * @return OrderVersionResponse
     * @throws OrderNotFoundException if version doesn't exist
     */
    public OrderVersionResponse getSpecificVersion(String orderId, Integer versionNumber) {
        log.debug("Fetching version {} for orderId: {}", versionNumber, orderId);

        OrderVersionedDocument document = orderVersionedRepository
            .findByOrderIdAndOrderVersionNumber(orderId, versionNumber)
            .orElseThrow(() -> new OrderNotFoundException(orderId, versionNumber));

        return mapToResponse(document);
    }

    /**
     * Get complete version history for an order.
     *
     * @param orderId the order ID
     * @return OrderVersionHistoryResponse with all versions
     * @throws OrderNotFoundException if order doesn't exist
     */
    public OrderVersionHistoryResponse getVersionHistory(String orderId) {
        log.debug("Fetching version history for orderId: {}", orderId);

        List<OrderVersionIndex> indexes = orderVersionIndexRepository
            .findByOrderIdOrderByOrderVersionNumberAsc(orderId);

        if (indexes.isEmpty()) {
            throw new OrderNotFoundException(orderId);
        }

        List<VersionSummaryDTO> versions = indexes.stream()
            .map(this::mapToSummaryDTO)
            .collect(Collectors.toList());

        long committedCount = versions.stream()
            .filter(v -> v.getOrderStatus() == OrderStatus.COMMITTED)
            .count();

        long wipCount = versions.stream()
            .filter(v -> v.getOrderStatus() == OrderStatus.WIP)
            .count();

        return OrderVersionHistoryResponse.builder()
            .orderId(orderId)
            .totalVersions(versions.size())
            .committedVersions((int) committedCount)
            .wipVersions((int) wipCount)
            .versions(versions)
            .build();
    }

    /**
     * Get only committed versions of an order.
     *
     * @param orderId the order ID
     * @return List of committed versions
     */
    public List<OrderVersionResponse> getCommittedVersions(String orderId) {
        log.debug("Fetching committed versions for orderId: {}", orderId);

        List<OrderVersionedDocument> documents = orderVersionedRepository
            .findCommittedVersions(orderId);

        return documents.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Map entity to response DTO.
     *
     * @param document the entity
     * @return OrderVersionResponse DTO
     */
    private OrderVersionResponse mapToResponse(OrderVersionedDocument document) {
        return OrderVersionResponse.builder()
            .orderId(document.getOrderId())
            .orderVersionNumber(document.getOrderVersionNumber())
            .formVersionId(document.getFormVersionId())
            .orderStatus(document.getOrderStatus())
            .userName(document.getUserName())
            .timestamp(document.getTimestamp())
            .isLatestVersion(document.getIsLatestVersion())
            .previousVersionNumber(document.getPreviousVersionNumber())
            .changeDescription(document.getChangeDescription())
            .data(document.getOrderData())
            .build();
    }

    /**
     * Map index to summary DTO (lightweight).
     *
     * @param index the index entity
     * @return VersionSummaryDTO
     */
    private VersionSummaryDTO mapToSummaryDTO(OrderVersionIndex index) {
        return VersionSummaryDTO.builder()
            .orderId(index.getOrderId())
            .orderVersionNumber(index.getOrderVersionNumber())
            .formVersionId(index.getFormVersionId())
            .orderStatus(index.getOrderStatus())
            .userName(index.getUserName())
            .timestamp(index.getTimestamp())
            .isLatestVersion(index.getIsLatestVersion())
            .changeDescription(index.getChangeDescription())
            .build();
    }
}
