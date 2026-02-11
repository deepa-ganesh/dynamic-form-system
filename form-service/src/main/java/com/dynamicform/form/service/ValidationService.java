package com.dynamicform.form.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.dynamicform.form.common.exception.ValidationException;
import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for validating order data against form schemas.
 *
 * Performs field-level validation based on schema definitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    /**
     * Validate order data against a form schema.
     *
     * @param data the order data to validate
     * @param schema the form schema to validate against
     * @throws ValidationException if validation fails
     */
    public void validateOrderData(Map<String, Object> data, FormSchemaEntity schema) {
        log.debug("Validating order data against schema: {}", schema.getFormVersionId());

        JsonNode fieldDefinitions = schema.getFieldDefinitions();
        JsonNode fields = fieldDefinitions.get("fields");

        if (fields == null || !fields.isArray()) {
            log.warn("Schema has no field definitions: {}", schema.getFormVersionId());
            return;
        }

        validateFields(data, fields, "");

        log.debug("Validation successful for schema: {}", schema.getFormVersionId());
    }

    /**
     * Recursively validate fields in current context map.
     *
     * @param contextData current object scope (root/subform/table row)
     * @param fields field definitions for this scope
     * @param pathPrefix path prefix for error messages
     */
    private void validateFields(Map<String, Object> contextData, JsonNode fields, String pathPrefix) {
        for (JsonNode fieldDef : fields) {
            String fieldName = fieldDef.get("fieldName").asText();
            String fieldType = fieldDef.get("fieldType").asText("");
            String fieldPath = pathPrefix.isEmpty() ? fieldName : pathPrefix + "." + fieldName;
            boolean required = fieldDef.has("required") && fieldDef.get("required").asBoolean();

            Object value = contextData.get(fieldName);

            if (required && isEmptyValue(value, fieldType)) {
                throw new ValidationException(fieldPath, "Field is required but missing");
            }

            if (!isEmptyValue(value, fieldType)) {
                validateFieldValue(fieldPath, value, fieldDef);
            }

            switch (fieldType) {
                case "subform":
                    validateSubform(fieldPath, value, fieldDef);
                    break;
                case "table":
                    validateTable(fieldPath, value, fieldDef);
                    break;
                default:
                    // no-op for simple fields
            }
        }
    }

    /**
     * Validate nested subform fields.
     */
    @SuppressWarnings("unchecked")
    private void validateSubform(String fieldPath, Object value, JsonNode fieldDef) {
        JsonNode subFields = fieldDef.get("subFields");
        if (subFields == null || !subFields.isArray() || value == null) {
            return;
        }

        if (!(value instanceof Map)) {
            throw new ValidationException(fieldPath, "Expected object value for subform");
        }

        validateFields((Map<String, Object>) value, subFields, fieldPath);
    }

    /**
     * Validate nested table rows and their column definitions.
     */
    @SuppressWarnings("unchecked")
    private void validateTable(String fieldPath, Object value, JsonNode fieldDef) {
        JsonNode columns = fieldDef.get("columns");
        if (columns == null || !columns.isArray() || value == null) {
            return;
        }

        if (!(value instanceof List)) {
            throw new ValidationException(fieldPath, "Expected array value for table");
        }

        List<?> rows = (List<?>) value;
        for (int i = 0; i < rows.size(); i++) {
            Object row = rows.get(i);
            String rowPath = fieldPath + "[" + i + "]";

            if (!(row instanceof Map)) {
                throw new ValidationException(rowPath, "Expected object row in table");
            }

            validateFields((Map<String, Object>) row, columns, rowPath);
        }
    }

    /**
     * Check whether value should be treated as empty for required checks.
     */
    private boolean isEmptyValue(Object value, String fieldType) {
        if (value == null) {
            return true;
        }

        switch (fieldType) {
            case "multivalue":
            case "table":
                return !(value instanceof List) || ((List<?>) value).isEmpty();
            case "subform":
                return !(value instanceof Map) || ((Map<?, ?>) value).isEmpty();
            case "checkbox":
                return false;
            default:
                return String.valueOf(value).trim().isEmpty();
        }
    }

    /**
     * Validate a single field value against its definition.
     *
     * @param fieldName the field name
     * @param value the field value
     * @param fieldDef the field definition from schema
     * @throws ValidationException if validation fails
     */
    private void validateFieldValue(String fieldName, Object value, JsonNode fieldDef) {
        String fieldType = fieldDef.get("fieldType").asText();

        switch (fieldType) {
            case "text":
                validateTextField(fieldName, value, fieldDef);
                break;
            case "number":
                validateNumberField(fieldName, value, fieldDef);
                break;
            case "multivalue":
                validateMultivalueField(fieldName, value, fieldDef);
                break;
            case "date":
                validateDateField(fieldName, value, fieldDef);
                break;
            // Add more validation types as needed
            default:
                log.debug("No specific validation for field type: {}", fieldType);
        }
    }

    /**
     * Validate text field.
     */
    private void validateTextField(String fieldName, Object value, JsonNode fieldDef) {
        if (!(value instanceof String)) {
            throw new ValidationException(fieldName, "Expected text value");
        }

        String textValue = (String) value;

        // Check pattern if defined
        if (fieldDef.has("validation") && fieldDef.get("validation").has("pattern")) {
            String pattern = fieldDef.get("validation").get("pattern").asText();
            if (!textValue.matches(pattern)) {
                throw new ValidationException(fieldName,
                    "Value does not match required pattern: " + pattern);
            }
        }

        // Check min/max length
        if (fieldDef.has("validation")) {
            JsonNode validation = fieldDef.get("validation");
            if (validation.has("minLength") && textValue.length() < validation.get("minLength").asInt()) {
                throw new ValidationException(fieldName, "Value is too short");
            }
            if (validation.has("maxLength") && textValue.length() > validation.get("maxLength").asInt()) {
                throw new ValidationException(fieldName, "Value is too long");
            }
        }
    }

    /**
     * Validate number field.
     */
    private void validateNumberField(String fieldName, Object value, JsonNode fieldDef) {
        if (!(value instanceof Number)) {
            throw new ValidationException(fieldName, "Expected numeric value");
        }

        double numValue = ((Number) value).doubleValue();

        // Check min/max
        if (fieldDef.has("validation")) {
            JsonNode validation = fieldDef.get("validation");
            if (validation.has("min") && numValue < validation.get("min").asDouble()) {
                throw new ValidationException(fieldName, "Value is below minimum");
            }
            if (validation.has("max") && numValue > validation.get("max").asDouble()) {
                throw new ValidationException(fieldName, "Value exceeds maximum");
            }
        }
    }

    /**
     * Validate multivalue field (array).
     */
    private void validateMultivalueField(String fieldName, Object value, JsonNode fieldDef) {
        if (!(value instanceof java.util.List)) {
            throw new ValidationException(fieldName, "Expected array value");
        }

        @SuppressWarnings("unchecked")
        java.util.List<Object> listValue = (java.util.List<Object>) value;

        // Check min/max values
        if (fieldDef.has("minValues") && listValue.size() < fieldDef.get("minValues").asInt()) {
            throw new ValidationException(fieldName, "Too few values");
        }
        if (fieldDef.has("maxValues") && listValue.size() > fieldDef.get("maxValues").asInt()) {
            throw new ValidationException(fieldName, "Too many values");
        }
    }

    /**
     * Validate date field.
     */
    private void validateDateField(String fieldName, Object value, JsonNode fieldDef) {
        if (!(value instanceof String)) {
            throw new ValidationException(fieldName, "Expected date string");
        }

        // Basic ISO date format validation
        String dateValue = (String) value;
        try {
            java.time.LocalDate.parse(dateValue);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidationException(fieldName, "Invalid date format. Expected ISO date (yyyy-MM-dd)");
        }
    }
}
