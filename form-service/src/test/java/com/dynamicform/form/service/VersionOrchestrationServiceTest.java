package com.dynamicform.form.service;

import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderVersionHistoryResponse;
import com.dynamicform.form.common.dto.OrderVersionResponse;
import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.common.exception.OrderNotFoundException;
import com.dynamicform.form.common.exception.SchemaNotFoundException;
import com.dynamicform.form.entity.mongo.OrderVersionIndex;
import com.dynamicform.form.entity.mongo.OrderVersionedDocument;
import com.dynamicform.form.entity.postgres.FormSchemaEntity;
import com.dynamicform.form.repository.mongo.OrderVersionIndexRepository;
import com.dynamicform.form.repository.mongo.OrderVersionedRepository;
import com.dynamicform.form.repository.postgres.FormSchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VersionOrchestrationService.
 *
 * Tests the CORE versioning logic:
 * - Creating first version (version 1)
 * - Creating subsequent versions (auto-increment)
 * - Managing isLatestVersion flags
 * - WIP vs COMMITTED status handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VersionOrchestrationService Tests")
class VersionOrchestrationServiceTest {

    @Mock
    private OrderVersionedRepository orderVersionedRepository;

    @Mock
    private OrderVersionIndexRepository orderVersionIndexRepository;

    @Mock
    private FormSchemaRepository formSchemaRepository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private VersionOrchestrationService versionOrchestrationService;

    private FormSchemaEntity activeSchema;
    private CreateOrderRequest createRequest;

    @BeforeEach
    void setUp() {
        activeSchema = FormSchemaEntity.builder()
            .id(1L)
            .formVersionId("v2.1.0")
            .formName("Order Form")
            .isActive(true)
            .build();

        createRequest = CreateOrderRequest.builder()
            .orderId("ORD-12345")
            .deliveryLocations(List.of("Location A", "Location B"))
            .data(Map.of("field1", "value1"))
            .finalSave(false)
            .changeDescription("Initial creation")
            .build();
    }

    @Test
    @DisplayName("Should create version 1 for new order")
    void shouldCreateFirstVersion() {
        // Arrange
        when(formSchemaRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeSchema));
        when(orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc(anyString()))
            .thenReturn(Optional.empty());

        OrderVersionedDocument savedDoc = OrderVersionedDocument.builder()
            .id("mongo-id-1")
            .orderId("ORD-12345")
            .orderVersionNumber(1)
            .formVersionId("v2.1.0")
            .orderStatus(OrderStatus.WIP)
            .isLatestVersion(true)
            .build();

        when(orderVersionedRepository.save(any())).thenReturn(savedDoc);

        // Act
        OrderVersionResponse response = versionOrchestrationService.createNewVersion(
            createRequest, "test.user@example.com"
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderVersionNumber()).isEqualTo(1);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.WIP);
        assertThat(response.getIsLatestVersion()).isTrue();
        assertThat(response.getPreviousVersionNumber()).isNull();

        verify(orderVersionedRepository, times(1)).save(any(OrderVersionedDocument.class));
        verify(orderVersionIndexRepository, times(1)).save(any(OrderVersionIndex.class));
    }

    @Test
    @DisplayName("Should create version 2 in insert-only mode")
    void shouldCreateSecondVersion() {
        // Arrange
        when(formSchemaRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeSchema));

        OrderVersionIndex existingIndex = OrderVersionIndex.builder()
            .orderId("ORD-12345")
            .orderVersionNumber(1)
            .isLatestVersion(true)
            .build();
        when(orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc("ORD-12345"))
            .thenReturn(Optional.of(existingIndex));

        OrderVersionedDocument savedDoc = OrderVersionedDocument.builder()
            .id("mongo-id-2")
            .orderId("ORD-12345")
            .orderVersionNumber(2)
            .formVersionId("v2.1.0")
            .orderStatus(OrderStatus.WIP)
            .isLatestVersion(true)
            .previousVersionNumber(1)
            .build();
        when(orderVersionedRepository.save(any())).thenReturn(savedDoc);

        // Act
        OrderVersionResponse response = versionOrchestrationService.createNewVersion(
            createRequest, "test.user@example.com"
        );

        // Assert
        assertThat(response.getOrderVersionNumber()).isEqualTo(2);
        assertThat(response.getPreviousVersionNumber()).isEqualTo(1);
        assertThat(response.getIsLatestVersion()).isTrue();
        verify(orderVersionedRepository, times(1)).save(any(OrderVersionedDocument.class));
        verify(orderVersionedRepository, never())
            .findByOrderIdAndOrderVersionNumber("ORD-12345", 1);
    }

    @Test
    @DisplayName("Should create COMMITTED version when finalSave is true")
    void shouldCreateCommittedVersion() {
        // Arrange
        createRequest.setFinalSave(true);

        when(formSchemaRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeSchema));
        when(orderVersionIndexRepository.findTopByOrderIdOrderByOrderVersionNumberDesc(anyString()))
            .thenReturn(Optional.empty());

        OrderVersionedDocument savedDoc = OrderVersionedDocument.builder()
            .id("mongo-id-1")
            .orderId("ORD-12345")
            .orderVersionNumber(1)
            .orderStatus(OrderStatus.COMMITTED)
            .isLatestVersion(true)
            .build();
        when(orderVersionedRepository.save(any())).thenReturn(savedDoc);

        // Act
        OrderVersionResponse response = versionOrchestrationService.createNewVersion(
            createRequest, "test.user@example.com"
        );

        // Assert
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.COMMITTED);
    }

    @Test
    @DisplayName("Should throw SchemaNotFoundException when no active schema")
    void shouldThrowExceptionWhenNoActiveSchema() {
        // Arrange
        when(formSchemaRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            versionOrchestrationService.createNewVersion(createRequest, "test.user@example.com")
        )
            .isInstanceOf(SchemaNotFoundException.class)
            .hasMessageContaining("No active form schema found");
    }

    @Test
    @DisplayName("Should get latest version successfully")
    void shouldGetLatestVersion() {
        // Arrange
        OrderVersionedDocument doc = OrderVersionedDocument.builder()
            .id("mongo-id-1")
            .orderId("ORD-12345")
            .orderVersionNumber(3)
            .formVersionId("v2.1.0")
            .orderStatus(OrderStatus.COMMITTED)
            .isLatestVersion(true)
            .orderData(Map.of("field1", "value1"))
            .build();

        when(orderVersionedRepository.findTopByOrderIdOrderByOrderVersionNumberDesc("ORD-12345"))
            .thenReturn(Optional.of(doc));

        // Act
        OrderVersionResponse response = versionOrchestrationService.getLatestVersion("ORD-12345");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderVersionNumber()).isEqualTo(3);
        assertThat(response.getIsLatestVersion()).isTrue();
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        when(orderVersionedRepository.findTopByOrderIdOrderByOrderVersionNumberDesc("ORD-99999"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            versionOrchestrationService.getLatestVersion("ORD-99999")
        )
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining("ORD-99999");
    }

    @Test
    @DisplayName("Should get version history with counts")
    void shouldGetVersionHistory() {
        // Arrange
        List<OrderVersionIndex> indexes = List.of(
            OrderVersionIndex.builder()
                .orderId("ORD-12345")
                .orderVersionNumber(1)
                .orderStatus(OrderStatus.WIP)
                .isLatestVersion(false)
                .build(),
            OrderVersionIndex.builder()
                .orderId("ORD-12345")
                .orderVersionNumber(2)
                .orderStatus(OrderStatus.WIP)
                .isLatestVersion(false)
                .build(),
            OrderVersionIndex.builder()
                .orderId("ORD-12345")
                .orderVersionNumber(3)
                .orderStatus(OrderStatus.COMMITTED)
                .isLatestVersion(true)
                .build()
        );

        when(orderVersionIndexRepository.findByOrderIdOrderByOrderVersionNumberAsc("ORD-12345"))
            .thenReturn(indexes);

        // Act
        OrderVersionHistoryResponse response =
            versionOrchestrationService.getVersionHistory("ORD-12345");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalVersions()).isEqualTo(3);
        assertThat(response.getCommittedVersions()).isEqualTo(1);
        assertThat(response.getWipVersions()).isEqualTo(2);
        assertThat(response.getVersions()).hasSize(3);
    }
}
