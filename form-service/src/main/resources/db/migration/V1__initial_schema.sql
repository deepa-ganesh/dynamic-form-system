-- Form Schemas Table
CREATE TABLE form_schemas (
    id BIGSERIAL PRIMARY KEY,
    form_version_id VARCHAR(20) NOT NULL UNIQUE,
    form_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_date TIMESTAMP NOT NULL,
    deprecated_date TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_date TIMESTAMP,
    field_definitions JSONB NOT NULL
);

CREATE INDEX idx_form_version_id ON form_schemas(form_version_id);
CREATE INDEX idx_is_active ON form_schemas(is_active);
CREATE INDEX idx_created_date ON form_schemas(created_date);

-- Field Mappings Table
CREATE TABLE field_mappings (
    id BIGSERIAL PRIMARY KEY,
    form_version_id VARCHAR(20) NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    source_column VARCHAR(100) NOT NULL,
    target_field_path VARCHAR(200) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    transformation_function VARCHAR(100),
    is_required BOOLEAN NOT NULL DEFAULT false,
    default_value VARCHAR(255),
    processing_order INTEGER DEFAULT 100,
    metadata JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    CONSTRAINT uk_form_source_target UNIQUE (form_version_id, source_table, source_column, target_field_path)
);

CREATE INDEX idx_form_version_mapping ON field_mappings(form_version_id);
CREATE INDEX idx_source_table ON field_mappings(source_table);
CREATE INDEX idx_target_field ON field_mappings(target_field_path);

-- Comments
COMMENT ON TABLE form_schemas IS 'Stores form schema definitions with versioning';
COMMENT ON TABLE field_mappings IS 'Stores field mapping configurations for dimensional table integration';
