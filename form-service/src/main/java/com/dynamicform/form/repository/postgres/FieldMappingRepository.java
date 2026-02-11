package com.dynamicform.form.repository.postgres;

import com.dynamicform.form.entity.postgres.FieldMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for FieldMappingEntity.
 *
 * Provides queries for field mapping configurations used in data transformation.
 */
@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMappingEntity, Long> {

    /**
     * Find all mappings for a specific form version.
     *
     * @param formVersionId the schema version ID
     * @return List of field mappings for the schema
     */
    List<FieldMappingEntity> findByFormVersionId(String formVersionId);

    /**
     * Find all active mappings for a form version.
     *
     * @param formVersionId the schema version ID
     * @param isActive true to find active mappings
     * @return List of active field mappings
     */
    List<FieldMappingEntity> findByFormVersionIdAndIsActive(
        String formVersionId,
        Boolean isActive
    );

    /**
     * Find mappings for a specific source table.
     *
     * @param sourceTable the dimensional table name
     * @return List of mappings from this source table
     */
    List<FieldMappingEntity> findBySourceTable(String sourceTable);

    /**
     * Find mappings for a specific source table and form version.
     *
     * @param formVersionId the schema version ID
     * @param sourceTable the dimensional table name
     * @return List of mappings
     */
    List<FieldMappingEntity> findByFormVersionIdAndSourceTable(
        String formVersionId,
        String sourceTable
    );

    /**
     * Find a specific mapping.
     *
     * @param formVersionId the schema version ID
     * @param sourceTable the source table
     * @param sourceColumn the source column
     * @param targetFieldPath the target JSON path
     * @return Optional containing the mapping
     */
    Optional<FieldMappingEntity> findByFormVersionIdAndSourceTableAndSourceColumnAndTargetFieldPath(
        String formVersionId,
        String sourceTable,
        String sourceColumn,
        String targetFieldPath
    );

    /**
     * Find all mappings ordered by processing order.
     * Used during data transformation to apply mappings in correct sequence.
     *
     * @param formVersionId the schema version ID
     * @return List of mappings ordered by processing order
     */
    @Query("SELECT f FROM FieldMappingEntity f WHERE f.formVersionId = ?1 AND f.isActive = true ORDER BY f.processingOrder ASC")
    List<FieldMappingEntity> findActiveMappingsOrderedByProcessingOrder(String formVersionId);

    /**
     * Count mappings for a form version.
     *
     * @param formVersionId the schema version ID
     * @return count of mappings
     */
    Long countByFormVersionId(String formVersionId);

    /**
     * Delete all mappings for a form version.
     * Used when deprecating a schema.
     *
     * @param formVersionId the schema version ID
     */
    void deleteByFormVersionId(String formVersionId);
}
