package com.dynamicform.form.service;

import com.dynamicform.form.common.exception.ValidationException;
import com.dynamicform.form.entity.postgres.FieldMappingEntity;
import com.dynamicform.form.repository.postgres.FieldMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        validateSqlIdentifier("sourceTable", sourceTable, true);
        validateSqlIdentifier("sourceKeyColumn", sourceKeyColumn, false);

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

        Map<String, Object> sourceData = executeSourceLookup(sql, sourceTable, sourceKeyColumn, sourceKeyValue);

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
     * List available prefill mappings for a schema version.
     *
     * Groups active mappings by source table and exposes mapped columns,
     * target paths, and suggested key columns for UI-assisted prefill.
     *
     * @param formVersionId the schema version
     * @return mapping metadata payload for UI
     */
    public Map<String, Object> listPrefillMappings(String formVersionId) {
        List<FieldMappingEntity> mappings = fieldMappingRepository
            .findActiveMappingsOrderedByProcessingOrder(formVersionId);

        Map<String, List<FieldMappingEntity>> groupedByTable = mappings.stream()
            .collect(Collectors.groupingBy(
                FieldMappingEntity::getSourceTable,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<Map<String, Object>> tables = new ArrayList<>();
        groupedByTable.forEach((sourceTable, tableMappings) -> {
            Set<String> sourceColumns = tableMappings.stream()
                .map(FieldMappingEntity::getSourceColumn)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            List<String> targetFieldPaths = tableMappings.stream()
                .map(FieldMappingEntity::getTargetFieldPath)
                .distinct()
                .sorted()
                .toList();

            List<String> suggestedKeyColumns = sourceColumns.stream()
                .filter(column -> {
                    String normalized = column.toLowerCase();
                    return "id".equals(normalized) || normalized.endsWith("_id");
                })
                .sorted(Comparator.naturalOrder())
                .toList();

            if (suggestedKeyColumns.isEmpty() && !sourceColumns.isEmpty()) {
                suggestedKeyColumns = List.of(sourceColumns.iterator().next());
            }

            Map<String, Object> tableInfo = new LinkedHashMap<>();
            tableInfo.put("sourceTable", sourceTable);
            tableInfo.put("sourceColumns", new ArrayList<>(sourceColumns));
            tableInfo.put("targetFieldPaths", targetFieldPaths);
            tableInfo.put("suggestedKeyColumns", suggestedKeyColumns);
            tableInfo.put("mappingCount", tableMappings.size());
            tables.add(tableInfo);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("formVersionId", formVersionId);
        response.put("totalMappings", mappings.size());
        response.put("tableCount", tables.size());
        response.put("tables", tables);
        return response;
    }

    /**
     * Build SELECT query for dimensional table.
     */
    private String buildSelectQuery(String tableName, String keyColumn,
                                    List<FieldMappingEntity> mappings) {
        StringBuilder sql = new StringBuilder("SELECT ");

        List<String> sourceColumns = mappings.stream()
            .map(FieldMappingEntity::getSourceColumn)
            .distinct()
            .toList();

        // Add all source columns
        for (int i = 0; i < sourceColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(sourceColumns.get(i));
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

    private Map<String, Object> executeSourceLookup(
            String sql,
            String sourceTable,
            String sourceKeyColumn,
            String sourceKeyValue) {
        try {
            return jdbcTemplate.queryForMap(sql, sourceKeyValue);
        } catch (EmptyResultDataAccessException ex) {
            throw new ValidationException(
                String.format(
                    "No source data found in table '%s' for %s='%s'",
                    sourceTable,
                    sourceKeyColumn,
                    sourceKeyValue
                )
            );
        } catch (BadSqlGrammarException ex) {
            throw mapSqlGrammarException(ex, sourceTable, sourceKeyColumn);
        } catch (DataAccessException ex) {
            throw new ValidationException(
                String.format(
                    "Unable to read prefill source data from table '%s'. Please verify table and column configuration.",
                    sourceTable
                )
            );
        }
    }

    private ValidationException mapSqlGrammarException(
            BadSqlGrammarException ex,
            String sourceTable,
            String sourceKeyColumn) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof SQLException sqlException) {
            String sqlState = sqlException.getSQLState();
            if ("42P01".equals(sqlState)) {
                return new ValidationException(
                    String.format(
                        "Prefill source table '%s' does not exist in the configured PostgreSQL database.",
                        sourceTable
                    )
                );
            }
            if ("42703".equals(sqlState)) {
                return new ValidationException(
                    String.format(
                        "Prefill key column '%s' does not exist in table '%s'.",
                        sourceKeyColumn,
                        sourceTable
                    )
                );
            }
        }

        return new ValidationException(
            String.format(
                "Invalid prefill query for table '%s'. Verify mapping columns and key column configuration.",
                sourceTable
            )
        );
    }

    private void validateSqlIdentifier(String fieldName, String identifier, boolean allowSchemaQualified) {
        String pattern = allowSchemaQualified
            ? "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?$"
            : "^[a-zA-Z_][a-zA-Z0-9_]*$";
        if (identifier == null || !identifier.matches(pattern)) {
            throw new ValidationException(
                String.format("Invalid SQL identifier for '%s': %s", fieldName, identifier)
            );
        }
    }
}
