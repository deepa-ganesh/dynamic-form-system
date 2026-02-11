package com.morganstanley.form.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.morganstanley.form.common.exception.ValidationException;
import com.morganstanley.form.entity.postgres.FormSchemaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        // Iterate through field definitions and validate
        for (JsonNode fieldDef : fields) {
            String fieldName = fieldDef.get("fieldName").asText();
            boolean required = fieldDef.has("required") && fieldDef.get("required").asBoolean();

            // Check required fields
            if (required && !data.containsKey(fieldName)) {
                throw new ValidationException(fieldName, "Field is required but missing");
            }

            // Validate field value if present
            if (data.containsKey(fieldName)) {
                Object value = data.get(fieldName);
                validateFieldValue(fieldName, value, fieldDef);
            }
        }

        log.debug("Validation successful for schema: {}", schema.getFormVersionId());
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
