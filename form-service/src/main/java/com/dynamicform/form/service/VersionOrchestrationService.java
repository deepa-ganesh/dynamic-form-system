package com.dynamicform.form.service;

import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderSummaryResponse;
import com.dynamicform.form.common.dto.OrderVersionHistoryResponse;
import com.dynamicform.form.common.dto.OrderVersionResponse;
import com.dynamicform.form.common.dto.VersionSummaryDTO;
import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.common.exception.OrderNotFoundException;
import com.dynamicform.form.common.exception.SchemaNotFoundException;
import com.dynamicform.form.common.exception.ValidationException;
import com.dynamicform.form.entity.mongo.OrderVersionIndex;
import com.dynamicform.form.entity.mongo.OrderVersionedDocument;
import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import com.dynamicform.form.repository.mongo.OrderLatestSummaryProjection;
import com.dynamicform.form.repository.mongo.OrderVersionIndexRepository;
import com.dynamicform.form.repository.mongo.OrderVersionedRepository;
import com.dynamicform.form.repository.postgres.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
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

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^ORD-[0-9]{5}$");
    private static final int MAX_ORDER_ID_GENERATION_ATTEMPTS = 20;

    private final OrderVersionedRepository orderVersionedRepository;
    private final OrderVersionIndexRepository orderVersionIndexRepository;
    private final FormSchemaRepository formSchemaRepository;
    private final ValidationService validationService;

    /**
     * List latest order snapshot for all orders.
     *
     * @return list of latest order summaries
     */
    public List<OrderSummaryResponse> listLatestOrders() {
        return orderVersionIndexRepository.findLatestOrderSummaries().stream()
            .map(this::mapToOrderSummary)
            .collect(Collectors.toList());
    }

    /**
     * Create a new version of an order.
     *
     * Algorithm:
     * 1. Get active form schema
     * 2. Determine next version number (query latest version + 1)
     * 3. Create new OrderVersionedDocument
     * 4. Set status (WIP or COMMITTED based on finalSave flag)
     * 5. Save to orders_versioned collection
     * 6. Create corresponding OrderVersionIndex entry
     * 7. Return response DTO
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

        OrderStatus orderStatus = request.isFinalSave() ? OrderStatus.COMMITTED : OrderStatus.WIP;
        String resolvedOrderId = resolveOrderId(request.getOrderId(), orderStatus);
        List<String> resolvedDeliveryLocations = normalizeDeliveryLocations(request.getDeliveryLocations());
        Map<String, Object> resolvedOrderData = request.getData() != null
            ? new HashMap<>(request.getData())
            : new HashMap<>();

        if (orderStatus == OrderStatus.COMMITTED) {
            validateFinalSaveRequest(resolvedOrderId, resolvedDeliveryLocations);
        }

        // Validate payload against active schema before persisting immutable version.
        // Include top-level request fields so schema rules for orderId/deliveryLocations can apply.
        if (orderStatus == OrderStatus.COMMITTED) {
            Map<String, Object> validationPayload = new HashMap<>(resolvedOrderData);
            validationPayload.put("orderId", resolvedOrderId);
            validationPayload.put("deliveryLocations", resolvedDeliveryLocations);
            validationService.validateOrderData(validationPayload, activeSchema);
        }

        // Step 2: Determine next version number
        Optional<OrderVersionIndex> latestVersionIndex =
            orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc(resolvedOrderId);

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

        // Step 3: Build new document
        OrderVersionedDocument newDocument = OrderVersionedDocument.builder()
            .orderId(resolvedOrderId)
            .orderVersionNumber(newVersionNumber)
            .formVersionId(formVersionId)
            .orderStatus(orderStatus)
            .userName(userName)
            .timestamp(LocalDateTime.now())
            .isLatestVersion(true)  // This is now the latest
            .previousVersionNumber(previousVersionNumber)
            .changeDescription(request.getChangeDescription())
            .orderData(resolvedOrderData)
            .build();

        // Step 5: Save to MongoDB
        OrderVersionedDocument savedDocument = orderVersionedRepository.save(newDocument);
        log.info("Saved version {} for orderId: {} with status: {}",
                 newVersionNumber, resolvedOrderId, orderStatus);

        // Step 6: Create index entry
        createVersionIndex(savedDocument);

        // Step 7: Return response DTO
        return mapToResponse(savedDocument, true);
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

    private String resolveOrderId(String orderId, OrderStatus orderStatus) {
        String normalizedOrderId = normalizeOrderId(orderId);

        if (orderStatus == OrderStatus.COMMITTED) {
            if (!isValidOrderId(normalizedOrderId)) {
                throw new ValidationException("orderId", "Order ID must be in format ORD-XXXXX");
            }
            return normalizedOrderId;
        }

        if (isValidOrderId(normalizedOrderId)) {
            return normalizedOrderId;
        }

        String generatedOrderId = generateDraftOrderId();
        log.debug("Generated draft orderId: {} (input was missing/invalid)", generatedOrderId);
        return generatedOrderId;
    }

    private void validateFinalSaveRequest(String orderId, List<String> deliveryLocations) {
        if (!isValidOrderId(orderId)) {
            throw new ValidationException("orderId", "Order ID must be in format ORD-XXXXX");
        }

        if (deliveryLocations.isEmpty()) {
            throw new ValidationException("deliveryLocations", "At least one delivery location is required");
        }

        if (deliveryLocations.size() > 10) {
            throw new ValidationException("deliveryLocations", "Between 1 and 10 delivery locations allowed");
        }
    }

    private List<String> normalizeDeliveryLocations(List<String> deliveryLocations) {
        if (deliveryLocations == null) {
            return Collections.emptyList();
        }

        return deliveryLocations.stream()
            .filter(location -> location != null && !location.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private String normalizeOrderId(String orderId) {
        return orderId == null ? "" : orderId.trim().toUpperCase();
    }

    private boolean isValidOrderId(String orderId) {
        return orderId != null && ORDER_ID_PATTERN.matcher(orderId).matches();
    }

    private String generateDraftOrderId() {
        for (int attempt = 0; attempt < MAX_ORDER_ID_GENERATION_ATTEMPTS; attempt++) {
            int nextValue = ThreadLocalRandom.current().nextInt(0, 100000);
            String candidate = String.format("ORD-%05d", nextValue);
            if (orderVersionIndexRepository.countByOrderId(candidate) == 0L) {
                return candidate;
            }
        }

        // Rare fallback; still keeps ORD-XXXXX format.
        int fallback = Math.abs((int) (System.currentTimeMillis() % 100000));
        return String.format("ORD-%05d", fallback);
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
            .findTopByOrderIdOrderByOrderVersionNumberDesc(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapToResponse(document, true);
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

        Integer latestVersionNumber = getLatestVersionNumber(orderId);
        boolean isLatest = latestVersionNumber != null
            && versionNumber != null
            && versionNumber.equals(latestVersionNumber);

        return mapToResponse(document, isLatest);
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

        Integer latestVersionNumber = indexes.stream()
            .map(OrderVersionIndex::getOrderVersionNumber)
            .filter(version -> version != null)
            .max(Comparator.naturalOrder())
            .orElse(null);

        List<VersionSummaryDTO> versions = indexes.stream()
            .map(index -> mapToSummaryDTO(index, latestVersionNumber))
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

        Integer latestVersionNumber = getLatestVersionNumber(orderId);

        List<OrderVersionedDocument> documents = orderVersionedRepository
            .findCommittedVersions(orderId);

        return documents.stream()
            .sorted(Comparator.comparing(OrderVersionedDocument::getOrderVersionNumber))
            .map(document -> mapToResponse(
                document,
                latestVersionNumber != null
                    && latestVersionNumber.equals(document.getOrderVersionNumber())
            ))
            .collect(Collectors.toList());
    }

    /**
     * Promote a specific WIP version to a new committed version.
     *
     * This operation is immutable: it creates a brand-new committed version
     * based on the selected WIP version data.
     *
     * @param orderId order ID
     * @param sourceVersionNumber source WIP version number
     * @param userName current user
     * @param changeDescription optional change description
     * @return newly created committed version
     */
    @Transactional
    public OrderVersionResponse promoteWipVersion(
            String orderId,
            Integer sourceVersionNumber,
            String userName,
            String changeDescription) {
        log.info(
            "Promoting WIP version. orderId={}, sourceVersion={}, user={}",
            orderId,
            sourceVersionNumber,
            userName
        );

        OrderVersionedDocument sourceDocument = orderVersionedRepository
            .findByOrderIdAndOrderVersionNumber(orderId, sourceVersionNumber)
            .orElseThrow(() -> new OrderNotFoundException(orderId, sourceVersionNumber));

        if (sourceDocument.getOrderStatus() != OrderStatus.WIP) {
            throw new ValidationException(
                String.format(
                    "Only WIP versions can be promoted. orderId=%s, version=%d is %s",
                    orderId,
                    sourceVersionNumber,
                    sourceDocument.getOrderStatus()
                )
            );
        }

        Optional<OrderVersionIndex> latestVersionIndex =
            orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc(orderId);

        int newVersionNumber = latestVersionIndex
            .map(index -> index.getOrderVersionNumber() + 1)
            .orElse(1);

        Integer previousVersionNumber = latestVersionIndex
            .map(OrderVersionIndex::getOrderVersionNumber)
            .orElse(null);

        Map<String, Object> sourceData = sourceDocument.getOrderData();
        String effectiveChangeDescription = changeDescription != null && !changeDescription.isBlank()
            ? changeDescription
            : String.format("Promoted from WIP version %d", sourceVersionNumber);

        OrderVersionedDocument promotedDocument = OrderVersionedDocument.builder()
            .orderId(orderId)
            .orderVersionNumber(newVersionNumber)
            .formVersionId(sourceDocument.getFormVersionId())
            .orderStatus(OrderStatus.COMMITTED)
            .userName(userName)
            .timestamp(LocalDateTime.now())
            .isLatestVersion(true)
            .previousVersionNumber(previousVersionNumber)
            .changeDescription(effectiveChangeDescription)
            .orderData(sourceData)
            .build();

        OrderVersionedDocument savedDocument = orderVersionedRepository.save(promotedDocument);
        createVersionIndex(savedDocument);

        log.info(
            "Promoted WIP version {} to committed version {} for orderId={}",
            sourceVersionNumber,
            newVersionNumber,
            orderId
        );

        return mapToResponse(savedDocument, true);
    }

    /**
     * Map entity to response DTO.
     *
     * @param document the entity
     * @return OrderVersionResponse DTO
     */
    private OrderVersionResponse mapToResponse(OrderVersionedDocument document, boolean isLatestVersion) {
        return OrderVersionResponse.builder()
            .orderId(document.getOrderId())
            .orderVersionNumber(document.getOrderVersionNumber())
            .formVersionId(document.getFormVersionId())
            .orderStatus(document.getOrderStatus())
            .userName(document.getUserName())
            .timestamp(document.getTimestamp())
            .isLatestVersion(isLatestVersion)
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
    private VersionSummaryDTO mapToSummaryDTO(OrderVersionIndex index, Integer latestVersionNumber) {
        return VersionSummaryDTO.builder()
            .orderId(index.getOrderId())
            .orderVersionNumber(index.getOrderVersionNumber())
            .formVersionId(index.getFormVersionId())
            .orderStatus(index.getOrderStatus())
            .userName(index.getUserName())
            .timestamp(index.getTimestamp())
            .isLatestVersion(
                latestVersionNumber != null
                    && latestVersionNumber.equals(index.getOrderVersionNumber())
            )
            .changeDescription(index.getChangeDescription())
            .build();
    }

    /**
     * Resolve latest version number using descending version ordering.
     *
     * @param orderId the order ID
     * @return latest version number or null when no versions exist
     */
    private Integer getLatestVersionNumber(String orderId) {
        return orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc(orderId)
            .map(OrderVersionIndex::getOrderVersionNumber)
            .orElse(null);
    }

    private OrderSummaryResponse mapToOrderSummary(OrderLatestSummaryProjection projection) {
        return OrderSummaryResponse.builder()
            .orderId(projection.getOrderId())
            .latestVersionNumber(projection.getLatestVersionNumber())
            .formVersionId(projection.getFormVersionId())
            .orderStatus(projection.getOrderStatus())
            .userName(projection.getUserName())
            .timestamp(projection.getTimestamp())
            .changeDescription(projection.getChangeDescription())
            .totalVersions(projection.getTotalVersions())
            .committedVersions(projection.getCommittedVersions())
            .wipVersions(projection.getWipVersions())
            .build();
    }
}
