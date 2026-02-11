package com.morganstanley.form.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morganstanley.form.common.dto.CreateSchemaRequest;
import com.morganstanley.form.common.dto.SchemaResponse;
import com.morganstanley.form.common.exception.SchemaNotFoundException;
import com.morganstanley.form.common.exception.SchemaVersionException;
import com.morganstanley.form.entity.postgres.FormSchemaEntity;
import com.morganstanley.form.repository.postgres.FormSchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SchemaManagementService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaManagementService Tests")
class SchemaManagementServiceTest {

    @Mock
    private FormSchemaRepository formSchemaRepository;

    @InjectMocks
    private SchemaManagementService schemaManagementService;

    private FormSchemaEntity schema;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        JsonNode fieldDefs = objectMapper.readTree("{\"fields\": []}");

        schema = FormSchemaEntity.builder()
            .id(1L)
            .formVersionId("v2.1.0")
            .formName("Test Form")
            .description("Test description")
            .isActive(true)
            .createdDate(LocalDateTime.now())
            .createdBy("test.user")
            .fieldDefinitions(fieldDefs)
            .build();
    }

    @Test
    @DisplayName("Should get schema by version ID")
    void shouldGetSchemaByVersionId() {
        // Arrange
        when(formSchemaRepository.findByFormVersionId("v2.1.0"))
            .thenReturn(Optional.of(schema));

        // Act
        SchemaResponse response = schemaManagementService.getSchemaByVersionId("v2.1.0");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFormVersionId()).isEqualTo("v2.1.0");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when schema not found")
    void shouldThrowExceptionWhenSchemaNotFound() {
        // Arrange
        when(formSchemaRepository.findByFormVersionId("v9.9.9"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            schemaManagementService.getSchemaByVersionId("v9.9.9")
        )
            .isInstanceOf(SchemaNotFoundException.class);
    }

    @Test
    @DisplayName("Should create new schema")
    void shouldCreateNewSchema() throws Exception {
        // Arrange
        JsonNode fieldDefs = objectMapper.readTree("{\"fields\": []}");
        CreateSchemaRequest request = CreateSchemaRequest.builder()
            .formVersionId("v3.0.0")
            .formName("New Form")
            .description("New schema")
            .fieldDefinitions(fieldDefs)
            .build();

        when(formSchemaRepository.existsByFormVersionId("v3.0.0")).thenReturn(false);
        when(formSchemaRepository.save(any())).thenReturn(schema);

        // Act
        SchemaResponse response = schemaManagementService.createNewSchema(request, "admin");

        // Assert
        assertThat(response).isNotNull();
        verify(formSchemaRepository, times(1)).save(any(FormSchemaEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when schema version already exists")
    void shouldThrowExceptionWhenVersionExists() throws Exception {
        // Arrange
        JsonNode fieldDefs = objectMapper.readTree("{\"fields\": []}");
        CreateSchemaRequest request = CreateSchemaRequest.builder()
            .formVersionId("v2.1.0")
            .formName("Duplicate")
            .fieldDefinitions(fieldDefs)
            .build();

        when(formSchemaRepository.existsByFormVersionId("v2.1.0")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
            schemaManagementService.createNewSchema(request, "admin")
        )
            .isInstanceOf(SchemaVersionException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should activate schema")
    void shouldActivateSchema() {
        // Arrange
        FormSchemaEntity oldActive = FormSchemaEntity.builder()
            .id(1L)
            .formVersionId("v2.0.0")
            .isActive(true)
            .build();

        FormSchemaEntity newActive = FormSchemaEntity.builder()
            .id(2L)
            .formVersionId("v2.1.0")
            .isActive(false)
            .build();

        when(formSchemaRepository.findByFormVersionId("v2.1.0"))
            .thenReturn(Optional.of(newActive));
        when(formSchemaRepository.findByIsActiveTrue())
            .thenReturn(Optional.of(oldActive));
        when(formSchemaRepository.save(any())).thenReturn(newActive);

        // Act
        SchemaResponse response = schemaManagementService.activateSchema("v2.1.0");

        // Assert
        assertThat(response).isNotNull();
        verify(formSchemaRepository, times(2)).save(any(FormSchemaEntity.class));
    }
}
