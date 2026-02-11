package com.dynamicform.form.repository.postgres;

import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for FormSchemaEntity.
 *
 * Provides CRUD operations and queries for form schema definitions.
 */
@Repository
public interface FormSchemaRepository extends JpaRepository<FormSchemaEntity, Long> {

    /**
     * Find a schema by its version ID.
     *
     * @param formVersionId the schema version ID (e.g., v2.1.0)
     * @return Optional containing the schema
     */
    Optional<FormSchemaEntity> findByFormVersionId(String formVersionId);

    /**
     * Find the currently active schema.
     * Only one schema should be active at a time.
     *
     * @return Optional containing the active schema
     */
    Optional<FormSchemaEntity> findByIsActiveTrue();

    /**
     * Find all schemas, ordered by creation date descending.
     *
     * @return List of all schemas (newest first)
     */
    List<FormSchemaEntity> findAllByOrderByCreatedDateDesc();

    /**
     * Find all active schemas.
     * Normally there should only be one, but this handles edge cases.
     *
     * @param isActive true to find active schemas
     * @return List of active schemas
     */
    List<FormSchemaEntity> findByIsActive(Boolean isActive);

    /**
     * Find schemas created within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return List of schemas created in the range
     */
    List<FormSchemaEntity> findByCreatedDateBetween(
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Find schemas by form name (case-insensitive).
     *
     * @param formName the form name to search for
     * @return List of schemas with matching name
     */
    List<FormSchemaEntity> findByFormNameContainingIgnoreCase(String formName);

    /**
     * Check if a schema version exists.
     *
     * @param formVersionId the schema version ID
     * @return true if exists, false otherwise
     */
    boolean existsByFormVersionId(String formVersionId);

    /**
     * Count total schemas.
     *
     * @return count of all schemas
     */
    @Query("SELECT COUNT(f) FROM FormSchemaEntity f")
    Long countAllSchemas();

    /**
     * Count active schemas.
     * Should normally return 1.
     *
     * @return count of active schemas
     */
    Long countByIsActiveTrue();
}
