package com.morganstanley.form.common.exception;

/**
 * Exception thrown when an order cannot be found.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }

    public OrderNotFoundException(String orderId, Integer versionNumber) {
        super(String.format("Order not found: %s, version: %d", orderId, versionNumber));
    }
}
