package com.dynamicform.form.common.enums;

/**
 * Supported dynamic form field types.
 */
public enum FieldType {

    /**
     * Single-line or multi-line textual input.
     */
    TEXT("text"),

    /**
     * Field containing multiple values of the same type.
     */
    MULTIVALUE("multivalue"),

    /**
     * Nested group of related fields.
     */
    SUBFORM("subform"),

    /**
     * Repeating row structure with defined columns.
     */
    TABLE("table"),

    /**
     * Value selected from an external reference source.
     */
    LOOKUP("lookup"),

    /**
     * Numeric input field.
     */
    NUMBER("number"),

    /**
     * Date-only input field.
     */
    DATE("date"),

    /**
     * Predefined choice list field.
     */
    DROPDOWN("dropdown"),

    /**
     * Boolean true/false input field.
     */
    CHECKBOX("checkbox"),

    /**
     * Read-only computed field derived from formulas.
     */
    CALCULATED("calculated");

    private final String value;

    FieldType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
