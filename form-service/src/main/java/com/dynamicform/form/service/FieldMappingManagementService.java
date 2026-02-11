package com.dynamicform.form.service;

import com.dynamicform.form.common.dto.FieldMappingRequest;
import com.dynamicform.form.common.dto.FieldMappingResponse;
import com.dynamicform.form.common.exception.FieldMappingNotFoundException;
import com.dynamicform.form.common.exception.SchemaNotFoundException;
import com.dynamicform.form.common.exception.ValidationException;
import com.dynamicform.form.entity.postgres.FieldMappingEntity;
import com.dynamicform.form.repository.postgres.FieldMappingRepository;
import com.dynamicform.form.repository.postgres.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing field mapping configurations.
 *
 * Handles CRUD operations for schema-specific dimensional-to-JSON mappings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldMappingManagementService {

    private final FieldMappingRepository fieldMappingRepository;
    private final FormSchemaRepository formSchemaRepository;

    /**
     * Get all mappings for a schema version ordered by processing order.
     *
     * @param formVersionId schema version ID
     * @return ordered field mappings
     */
    public List<FieldMappingResponse> getMappingsBySchema(String formVersionId) {
        validateSchemaExists(formVersionId);
        return fieldMappingRepository.findByFormVersionIdOrderByProcessingOrderAscIdAsc(formVersionId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Create a new field mapping for a schema.
     *
     * @param formVersionId schema version ID
     * @param request create request
     * @param userName authenticated user
     * @return created mapping
     */
    @Transactional
    public FieldMappingResponse createMapping(
            String formVersionId,
            FieldMappingRequest request,
            String userName) {
        validateSchemaExists(formVersionId);
        validateUniqueness(formVersionId, request, null);

        FieldMappingEntity entity = FieldMappingEntity.builder()
            .formVersionId(formVersionId)
            .sourceTable(request.getSourceTable())
            .sourceColumn(request.getSourceColumn())
            .targetFieldPath(request.getTargetFieldPath())
            .dataType(request.getDataType())
            .transformationFunction(request.getTransformationFunction())
            .isRequired(Boolean.TRUE.equals(request.getIsRequired()))
            .defaultValue(request.getDefaultValue())
            .processingOrder(request.getProcessingOrder() != null ? request.getProcessingOrder() : 100)
            .metadata(request.getMetadata())
            .isActive(request.getIsActive() == null || request.getIsActive())
            .createdBy(userName)
            .build();

        FieldMappingEntity savedEntity = fieldMappingRepository.save(entity);
        log.info(
            "Created field mapping id={} for schema={} source={}.{} target={}",
            savedEntity.getId(),
            formVersionId,
            savedEntity.getSourceTable(),
            savedEntity.getSourceColumn(),
            savedEntity.getTargetFieldPath()
        );
        return mapToResponse(savedEntity);
    }

    /**
     * Update an existing mapping.
     *
     * @param formVersionId schema version ID
     * @param mappingId mapping ID
     * @param request update request
     * @return updated mapping
     */
    @Transactional
    public FieldMappingResponse updateMapping(
            String formVersionId,
            Long mappingId,
            FieldMappingRequest request) {
        validateSchemaExists(formVersionId);

        FieldMappingEntity entity = fieldMappingRepository.findByIdAndFormVersionId(mappingId, formVersionId)
            .orElseThrow(() -> new FieldMappingNotFoundException(formVersionId, mappingId));

        validateUniqueness(formVersionId, request, mappingId);

        entity.setSourceTable(request.getSourceTable());
        entity.setSourceColumn(request.getSourceColumn());
        entity.setTargetFieldPath(request.getTargetFieldPath());
        entity.setDataType(request.getDataType());
        entity.setTransformationFunction(request.getTransformationFunction());
        entity.setIsRequired(Boolean.TRUE.equals(request.getIsRequired()));
        entity.setDefaultValue(request.getDefaultValue());
        entity.setProcessingOrder(request.getProcessingOrder() != null ? request.getProcessingOrder() : 100);
        entity.setMetadata(request.getMetadata());
        entity.setIsActive(request.getIsActive() == null || request.getIsActive());

        FieldMappingEntity savedEntity = fieldMappingRepository.save(entity);
        log.info("Updated field mapping id={} for schema={}", mappingId, formVersionId);
        return mapToResponse(savedEntity);
    }

    /**
     * Delete a mapping.
     *
     * @param formVersionId schema version ID
     * @param mappingId mapping ID
     */
    @Transactional
    public void deleteMapping(String formVersionId, Long mappingId) {
        validateSchemaExists(formVersionId);
        FieldMappingEntity entity = fieldMappingRepository.findByIdAndFormVersionId(mappingId, formVersionId)
            .orElseThrow(() -> new FieldMappingNotFoundException(formVersionId, mappingId));
        fieldMappingRepository.delete(entity);
        log.info("Deleted field mapping id={} for schema={}", mappingId, formVersionId);
    }

    private void validateSchemaExists(String formVersionId) {
        if (!formSchemaRepository.existsByFormVersionId(formVersionId)) {
            throw new SchemaNotFoundException(formVersionId);
        }
    }

    private void validateUniqueness(
            String formVersionId,
            FieldMappingRequest request,
            Long currentMappingId) {
        fieldMappingRepository
            .findByFormVersionIdAndSourceTableAndSourceColumnAndTargetFieldPath(
                formVersionId,
                request.getSourceTable(),
                request.getSourceColumn(),
                request.getTargetFieldPath()
            )
            .ifPresent(existing -> {
                if (currentMappingId == null || !existing.getId().equals(currentMappingId)) {
                    throw new ValidationException(
                        "A mapping already exists for this source table/column and target field path"
                    );
                }
            });
    }

    private FieldMappingResponse mapToResponse(FieldMappingEntity entity) {
        return FieldMappingResponse.builder()
            .id(entity.getId())
            .formVersionId(entity.getFormVersionId())
            .sourceTable(entity.getSourceTable())
            .sourceColumn(entity.getSourceColumn())
            .targetFieldPath(entity.getTargetFieldPath())
            .dataType(entity.getDataType())
            .transformationFunction(entity.getTransformationFunction())
            .isRequired(entity.getIsRequired())
            .defaultValue(entity.getDefaultValue())
            .processingOrder(entity.getProcessingOrder())
            .metadata(entity.getMetadata())
            .isActive(entity.getIsActive())
            .createdDate(entity.getCreatedDate())
            .createdBy(entity.getCreatedBy())
            .build();
    }
}
