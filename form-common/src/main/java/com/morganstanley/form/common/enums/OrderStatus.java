package com.morganstanley.form.common.enums;

/**
 * Represents order workflow and version states.
 */
public enum OrderStatus {

    /**
     * Work-in-progress version created during auto-save.
     */
    WIP("Work In Progress"),

    /**
     * Finalized version retained for audit and history.
     */
    COMMITTED("Committed"),

    /**
     * User-created draft state before submission.
     */
    DRAFT("Draft"),

    /**
     * Order submitted for downstream processing.
     */
    SUBMITTED("Submitted"),

    /**
     * Order approved by authorized reviewer.
     */
    APPROVED("Approved"),

    /**
     * Order cancelled and no longer active.
     */
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
