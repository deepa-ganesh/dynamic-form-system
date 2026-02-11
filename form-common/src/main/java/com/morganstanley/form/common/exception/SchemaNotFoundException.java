package com.morganstanley.form.common.exception;

/**
 * Exception thrown when a form schema cannot be found.
 */
public class SchemaNotFoundException extends RuntimeException {

    public SchemaNotFoundException(String formVersionId) {
        super("Form schema not found: " + formVersionId);
    }

    /**
     * Creates an exception with a preformatted message.
     *
     * @param message raw exception message
     * @return schema not found exception with custom message
     */
    public static SchemaNotFoundException withMessage(String message) {
        return new SchemaNotFoundException(message, true);
    }

    private SchemaNotFoundException(String message, boolean rawMessage) {
        super(message);
    }
}
