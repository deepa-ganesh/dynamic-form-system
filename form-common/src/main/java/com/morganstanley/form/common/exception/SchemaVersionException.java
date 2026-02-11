package com.morganstanley.form.common.exception;

/**
 * Exception thrown when schema version operations fail.
 */
public class SchemaVersionException extends RuntimeException {

    public SchemaVersionException(String message) {
        super(message);
    }

    public SchemaVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
