package com.dynamicform.form.service;

import com.dynamicform.form.entity.postgres.FieldMappingEntity;
import com.dynamicform.form.repository.postgres.FieldMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for transforming data from dimensional tables to JSON format.
 *
 * Applies field mappings to populate form data from legacy SQL tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataTransformationService {

    private final FieldMappingRepository fieldMappingRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Transform data from dimensional table to JSON format.
     *
     * @param formVersionId the schema version
     * @param sourceTable the dimensional table name
     * @param sourceKeyColumn the primary key column
     * @param sourceKeyValue the primary key value
     * @return Map containing transformed data
     */
    public Map<String, Object> transformDimensionalToJSON(
            String formVersionId,
            String sourceTable,
            String sourceKeyColumn,
            String sourceKeyValue) {

        log.debug("Transforming data from table: {}, key: {}", sourceTable, sourceKeyValue);

        // Get field mappings for this table and form version
        List<FieldMappingEntity> mappings = fieldMappingRepository
            .findActiveMappingsOrderedByProcessingOrder(formVersionId);

        // Filter mappings for this source table
        List<FieldMappingEntity> tableMappings = mappings.stream()
            .filter(m -> m.getSourceTable().equals(sourceTable))
            .toList();

        if (tableMappings.isEmpty()) {
            log.warn("No field mappings found for table: {} and formVersion: {}",
                    sourceTable, formVersionId);
            return new HashMap<>();
        }

        // Build SQL query to fetch data
        String sql = buildSelectQuery(sourceTable, sourceKeyColumn, tableMappings);

        // Execute query
        Map<String, Object> sourceData = jdbcTemplate.queryForMap(
            sql,
            sourceKeyValue
        );

        // Apply transformations and build result
        Map<String, Object> result = new HashMap<>();

        for (FieldMappingEntity mapping : tableMappings) {
            String sourceColumn = mapping.getSourceColumn();
            String targetPath = mapping.getTargetFieldPath();
            Object sourceValue = sourceData.get(sourceColumn);

            // Apply transformation if defined
            Object transformedValue = applyTransformation(sourceValue, mapping);

            // Set value in result using dot notation path
            setNestedValue(result, targetPath, transformedValue);
        }

        log.debug("Transformation complete. Generated {} fields", result.size());
        return result;
    }

    /**
     * Build SELECT query for dimensional table.
     */
    private String buildSelectQuery(String tableName, String keyColumn,
                                    List<FieldMappingEntity> mappings) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // Add all source columns
        for (int i = 0; i < mappings.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(mappings.get(i).getSourceColumn());
        }

        sql.append(" FROM ").append(tableName);
        sql.append(" WHERE ").append(keyColumn).append(" = ?");

        return sql.toString();
    }

    /**
     * Apply transformation function to a value.
     */
    private Object applyTransformation(Object value, FieldMappingEntity mapping) {
        if (value == null) {
            return mapping.getDefaultValue();
        }

        String transformFunction = mapping.getTransformationFunction();
        if (transformFunction == null) {
            return value;
        }

        // Apply transformation based on function name
        switch (transformFunction.toLowerCase()) {
            case "uppercase":
                return value.toString().toUpperCase();
            case "lowercase":
                return value.toString().toLowerCase();
            case "trim":
                return value.toString().trim();
            default:
                log.warn("Unknown transformation function: {}", transformFunction);
                return value;
        }
    }

    /**
     * Set value in nested map using dot notation.
     * Example: "deliveryCompany.name" sets result["deliveryCompany"]["name"]
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> result, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = result;

        // Navigate to the parent
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(part);
        }

        // Set the final value
        current.put(parts[parts.length - 1], value);
    }
}
