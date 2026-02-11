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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA entity for form schema definitions.
 *
 * Stores the structure of forms including field definitions, validation rules,
 * and UI layout information. Each schema version is immutable once activated.
 *
 * Table: form_schemas
 */
@Entity
@Table(
    name = "form_schemas",
    indexes = {
        @Index(name = "idx_form_version_id", columnList = "form_version_id", unique = true),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_created_date", columnList = "created_date")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchemaEntity {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique version identifier for this schema.
     * Format: vX.Y.Z (semantic versioning)
     * Example: v1.0.0, v2.1.0, v3.0.0
     */
    @Column(name = "form_version_id", nullable = false, unique = true, length = 20)
    private String formVersionId;

    /**
     * Human-readable name of the form.
     * Example: "Order Entry Form", "Customer Registration"
     */
    @Column(name = "form_name", nullable = false, length = 100)
    private String formName;

    /**
     * Optional description of the form and its purpose.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Flag indicating if this schema version is currently active.
     * Only one version should be active at a time.
     * New orders use the active schema.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * Timestamp when this schema was created.
     * Auto-populated by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Timestamp when this schema was deprecated.
     * Null if still in use.
     */
    @Column(name = "deprecated_date")
    private LocalDateTime deprecatedDate;

    /**
     * User who created this schema.
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Last modification timestamp.
     * Auto-updated by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    /**
     * Complete field definitions as JSON.
     * Stored as JSONB in PostgreSQL for efficient querying.
     *
     * Structure:
     * {
     *   "fields": [
     *     {
     *       "fieldName": "orderId",
     *       "fieldType": "text",
     *       "label": "Order ID",
     *       "required": true,
     *       "validation": {
     *         "pattern": "^ORD-[0-9]{5}$"
     *       }
     *     },
     *     {
     *       "fieldName": "deliveryLocations",
     *       "fieldType": "multivalue",
     *       "label": "Delivery Locations",
     *       "minValues": 1,
     *       "maxValues": 10
     *     },
     *     {
     *       "fieldName": "deliveryCompany",
     *       "fieldType": "subform",
     *       "label": "Delivery Company",
     *       "subFields": [
     *         {
     *           "fieldName": "companyId",
     *           "fieldType": "lookup",
     *           "lookupTable": "delivery_companies"
     *         }
     *       ]
     *     },
     *     {
     *       "fieldName": "items",
     *       "fieldType": "table",
     *       "label": "Order Items",
     *       "columns": [
     *         {
     *           "fieldName": "itemNumber",
     *           "fieldType": "lookup",
     *           "lookupTable": "products"
     *         },
     *         {
     *           "fieldName": "quantity",
     *           "fieldType": "number"
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "field_definitions", columnDefinition = "jsonb", nullable = false)
    private JsonNode fieldDefinitions;
}
