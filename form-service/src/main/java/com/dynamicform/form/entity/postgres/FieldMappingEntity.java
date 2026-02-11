package com.dynamicform.form.entity.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA entity for field mapping configurations.
 *
 * Defines how to transform data from dimensional (SQL) tables into JSON format
 * for storage in MongoDB. Supports denormalization and lookup population.
 *
 * Table: field_mappings
 */
@Entity
@Table(
    name = "field_mappings",
    indexes = {
        @Index(name = "idx_form_version_mapping", columnList = "form_version_id"),
        @Index(name = "idx_source_table", columnList = "source_table"),
        @Index(name = "idx_target_field", columnList = "target_field_path")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_form_source_target",
            columnNames = {"form_version_id", "source_table", "source_column", "target_field_path"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingEntity {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Form schema version this mapping applies to.
     * Example: v2.1.0
     */
    @Column(name = "form_version_id", nullable = false, length = 20)
    private String formVersionId;

    /**
     * Source dimensional table name.
     * Example: "delivery_companies", "products", "customers"
     */
    @Column(name = "source_table", nullable = false, length = 100)
    private String sourceTable;

    /**
     * Source column name in the dimensional table.
     * Example: "company_name", "contact_person", "phone"
     */
    @Column(name = "source_column", nullable = false, length = 100)
    private String sourceColumn;

    /**
     * Target field path in JSON using dot notation.
     * Example: "deliveryCompany.name", "deliveryCompany.contact", "items.price"
     */
    @Column(name = "target_field_path", nullable = false, length = 200)
    private String targetFieldPath;

    /**
     * Data type of the source column.
     * Example: "string", "integer", "decimal", "date"
     */
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * Optional transformation function to apply.
     * Example: "uppercase", "trim", "formatPhone", "formatCurrency"
     */
    @Column(name = "transformation_function", length = 100)
    private String transformationFunction;

    /**
     * Flag indicating if this field is required.
     */
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Default value if source is null.
     * Stored as string, converted to appropriate type during transformation.
     */
    @Column(name = "default_value", length = 255)
    private String defaultValue;

    /**
     * Priority/order for processing (lower numbers processed first).
     * Useful when one field depends on another.
     */
    @Column(name = "processing_order")
    @Builder.Default
    private Integer processingOrder = 100;

    /**
     * Additional metadata as JSON.
     * Can store validation rules, formatting options, etc.
     *
     * Example:
     * {
     *   "validation": {
     *     "minLength": 5,
     *     "maxLength": 50,
     *     "pattern": "^[A-Z0-9-]+$"
     *   },
     *   "formatting": {
     *     "decimalPlaces": 2,
     *     "currencySymbol": "$"
     *   }
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    /**
     * Flag indicating if this mapping is currently active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when this mapping was created.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * User who created this mapping.
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
