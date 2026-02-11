package com.dynamicform.form.common.exception;

/**
 * Exception thrown when a version conflict occurs.
 */
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(String orderId, Integer expectedVersion, Integer actualVersion) {
        super(String.format(
            "Version conflict for order %s. Expected version: %d, actual version: %d",
            orderId,
            expectedVersion,
            actualVersion
        ));
    }

    public VersionConflictException(String message) {
        super(message);
    }
}
