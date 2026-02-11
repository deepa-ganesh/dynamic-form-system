package com.dynamicform.form.common.exception;

/**
 * Exception thrown when a field mapping cannot be found.
 */
public class FieldMappingNotFoundException extends RuntimeException {

    public FieldMappingNotFoundException(Long mappingId) {
        super("Field mapping not found: " + mappingId);
    }

    public FieldMappingNotFoundException(String formVersionId, Long mappingId) {
        super(String.format("Field mapping not found: id=%d, formVersionId=%s", mappingId, formVersionId));
    }
}
