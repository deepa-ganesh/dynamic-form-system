package com.dynamicform.form.service;

import com.dynamicform.form.common.dto.CreateSchemaRequest;
import com.dynamicform.form.common.dto.SchemaResponse;
import com.dynamicform.form.common.exception.SchemaNotFoundException;
import com.dynamicform.form.common.exception.SchemaVersionException;
import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import com.dynamicform.form.repository.postgres.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing form schemas.
 *
 * Handles schema CRUD operations, versioning, activation, and caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaManagementService {

    private final FormSchemaRepository formSchemaRepository;

    /**
     * Get schema by version ID.
     * Results are cached for performance.
     *
     * @param formVersionId the schema version ID (e.g., v2.1.0)
     * @return SchemaResponse
     * @throws SchemaNotFoundException if schema doesn't exist
     */
    @Cacheable(value = "schemas", key = "#formVersionId")
    public SchemaResponse getSchemaByVersionId(String formVersionId) {
        log.debug("Fetching schema: {}", formVersionId);

        FormSchemaEntity entity = formSchemaRepository.findByFormVersionId(formVersionId)
            .orElseThrow(() -> new SchemaNotFoundException(formVersionId));

        return mapToResponse(entity);
    }

    /**
     * Get the currently active schema.
     * Results are cached.
     *
     * @return SchemaResponse for active schema
     * @throws SchemaNotFoundException if no active schema exists
     */
    @Cacheable(value = "activeSchema")
    public SchemaResponse getActiveSchema() {
        log.debug("Fetching active schema");

        FormSchemaEntity entity = formSchemaRepository.findByIsActiveTrue()
            .orElseThrow(() -> new SchemaNotFoundException("No active schema found"));

        return mapToResponse(entity);
    }

    /**
     * Get all schemas, ordered by creation date.
     *
     * @return List of all schemas
     */
    public List<SchemaResponse> getAllSchemas() {
        log.debug("Fetching all schemas");

        List<FormSchemaEntity> entities = formSchemaRepository.findAllByOrderByCreatedDateDesc();

        return entities.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Create a new form schema.
     * The new schema is NOT automatically activated.
     *
     * @param request the schema creation request
     * @param userName the user creating the schema
     * @return SchemaResponse for the created schema
     * @throws SchemaVersionException if schema version already exists
     */
    @Transactional
    @CacheEvict(value = {"schemas", "activeSchema"}, allEntries = true)
    public SchemaResponse createNewSchema(CreateSchemaRequest request, String userName) {
        log.info("Creating new schema: {}", request.getFormVersionId());

        // Check if version already exists
        if (formSchemaRepository.existsByFormVersionId(request.getFormVersionId())) {
            throw new SchemaVersionException(
                "Schema version already exists: " + request.getFormVersionId()
            );
        }

        FormSchemaEntity entity = FormSchemaEntity.builder()
            .formVersionId(request.getFormVersionId())
            .formName(request.getFormName())
            .description(request.getDescription())
            .isActive(false)  // New schemas are inactive by default
            .createdDate(LocalDateTime.now())
            .createdBy(userName)
            .fieldDefinitions(request.getFieldDefinitions())
            .build();

        FormSchemaEntity savedEntity = formSchemaRepository.save(entity);
        log.info("Created schema: {} by {}", savedEntity.getFormVersionId(), userName);

        return mapToResponse(savedEntity);
    }

    /**
     * Activate a schema version.
     * Deactivates the currently active schema and activates the specified one.
     *
     * @param formVersionId the schema version to activate
     * @return SchemaResponse for the activated schema
     * @throws SchemaNotFoundException if schema doesn't exist
     */
    @Transactional
    @CacheEvict(value = {"schemas", "activeSchema"}, allEntries = true)
    public SchemaResponse activateSchema(String formVersionId) {
        log.info("Activating schema: {}", formVersionId);

        // Find the schema to activate
        FormSchemaEntity schemaToActivate = formSchemaRepository.findByFormVersionId(formVersionId)
            .orElseThrow(() -> new SchemaNotFoundException(formVersionId));

        // Deactivate currently active schema
        formSchemaRepository.findByIsActiveTrue().ifPresent(activeSchema -> {
            log.info("Deactivating current schema: {}", activeSchema.getFormVersionId());
            activeSchema.setIsActive(false);
            activeSchema.setDeprecatedDate(LocalDateTime.now());
            formSchemaRepository.save(activeSchema);
        });

        // Activate new schema
        schemaToActivate.setIsActive(true);
        schemaToActivate.setDeprecatedDate(null);
        FormSchemaEntity savedEntity = formSchemaRepository.save(schemaToActivate);

        log.info("Activated schema: {}", formVersionId);
        return mapToResponse(savedEntity);
    }

    /**
     * Deprecate a schema (soft delete).
     * The schema remains in the database but is marked as deprecated.
     *
     * @param formVersionId the schema version to deprecate
     * @throws SchemaVersionException if trying to deprecate active schema
     */
    @Transactional
    @CacheEvict(value = {"schemas", "activeSchema"}, allEntries = true)
    public void deprecateSchema(String formVersionId) {
        log.info("Deprecating schema: {}", formVersionId);

        FormSchemaEntity entity = formSchemaRepository.findByFormVersionId(formVersionId)
            .orElseThrow(() -> new SchemaNotFoundException(formVersionId));

        if (entity.getIsActive()) {
            throw new SchemaVersionException("Cannot deprecate active schema. Activate another schema first.");
        }

        entity.setDeprecatedDate(LocalDateTime.now());
        formSchemaRepository.save(entity);

        log.info("Deprecated schema: {}", formVersionId);
    }

    /**
     * Map entity to response DTO.
     *
     * @param entity the entity
     * @return SchemaResponse DTO
     */
    private SchemaResponse mapToResponse(FormSchemaEntity entity) {
        return SchemaResponse.builder()
            .id(entity.getId())
            .formVersionId(entity.getFormVersionId())
            .formName(entity.getFormName())
            .description(entity.getDescription())
            .isActive(entity.getIsActive())
            .createdDate(entity.getCreatedDate())
            .deprecatedDate(entity.getDeprecatedDate())
            .createdBy(entity.getCreatedBy())
            .fieldDefinitions(entity.getFieldDefinitions())
            .build();
    }
}
