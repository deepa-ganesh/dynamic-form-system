package com.dynamicform.form.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dynamicform.form.common.dto.CreateOrderRequest;
import com.dynamicform.form.common.dto.OrderVersionResponse;
import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.service.VersionOrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for OrderController.
 *
 * Tests the web request/response cycle for Order endpoints.
 */
@WebMvcTest(OrderController.class)
@Import(com.dynamicform.form.security.SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("OrderController Integration Tests")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VersionOrchestrationService versionOrchestrationService;

    @Test
    @DisplayName("Should require authentication for order endpoints")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v1/orders/ORD-12345"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("Should create order with valid request")
    void shouldCreateOrderWithValidRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderId("ORD-12345")
            .deliveryLocations(List.of("Location A", "Location B"))
            .data(Map.of("field1", "value1"))
            .finalSave(false)
            .changeDescription("Test order")
            .build();

        OrderVersionResponse mockedResponse = OrderVersionResponse.builder()
            .orderId("ORD-12345")
            .orderVersionNumber(1)
            .orderStatus(OrderStatus.WIP)
            .isLatestVersion(true)
            .build();
        when(versionOrchestrationService.createNewVersion(any(CreateOrderRequest.class), anyString()))
            .thenReturn(mockedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("ORD-12345"))
            .andExpect(jsonPath("$.orderVersionNumber").value(1))
            .andExpect(jsonPath("$.orderStatus").value("WIP"))
            .andExpect(jsonPath("$.isLatestVersion").value(true));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("Should return 400 for invalid order request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Arrange - missing required orderId
        CreateOrderRequest request = CreateOrderRequest.builder()
            .deliveryLocations(List.of("Location A"))
            .data(Map.of("field1", "value1"))
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
}
