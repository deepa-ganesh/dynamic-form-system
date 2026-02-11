package com.dynamicform.form.batch.job;

import com.dynamicform.form.common.enums.OrderStatus;
import com.dynamicform.form.repository.mongo.OrderVersionIndexRepository;
import com.dynamicform.form.repository.mongo.OrderVersionedRepository;
import com.dynamicform.form.repository.mongo.PurgeAuditLogRepository;
import com.dynamicform.form.repository.mongo.WipVersionsGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PurgeTasklet.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurgeTasklet Tests")
class PurgeTaskletTest {

    @Mock
    private OrderVersionedRepository orderVersionedRepository;

    @Mock
    private OrderVersionIndexRepository orderVersionIndexRepository;

    @Mock
    private PurgeAuditLogRepository purgeAuditLogRepository;

    @InjectMocks
    private PurgeTasklet purgeTasklet;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Test
    @DisplayName("Should purge old WIP versions and keep latest")
    void shouldPurgeOldWipVersions() {
        // Arrange
        WipVersionsGroup wipGroup = WipVersionsGroup.builder()
            .orderId("ORD-12345")
            .wipVersions(List.of(1, 2, 3, 4, 5))
            .build();

        when(orderVersionIndexRepository.findOrdersWithWipVersions())
            .thenReturn(List.of(wipGroup));

        when(orderVersionedRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
            anyString(), anyList(), any(OrderStatus.class)
        )).thenReturn(4L);

        when(orderVersionIndexRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
            anyString(), anyList(), any(OrderStatus.class)
        )).thenReturn(4L);

        when(orderVersionIndexRepository.countByOrderIdAndOrderStatus(
            anyString(), any(OrderStatus.class)
        )).thenReturn(0L);

        // Act
        RepeatStatus status = purgeTasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<List<Integer>> versionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderVersionedRepository, times(1))
            .deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                eq("ORD-12345"),
                versionsCaptor.capture(),
                eq(OrderStatus.WIP)
            );
        assertThat(versionsCaptor.getValue()).containsExactly(4, 3, 2, 1);

        verify(purgeAuditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should handle single WIP version (nothing to delete)")
    void shouldHandleSingleWipVersion() {
        // Arrange
        WipVersionsGroup wipGroup = WipVersionsGroup.builder()
            .orderId("ORD-99999")
            .wipVersions(List.of(1))
            .build();

        when(orderVersionIndexRepository.findOrdersWithWipVersions())
            .thenReturn(List.of(wipGroup));

        when(orderVersionIndexRepository.countByOrderIdAndOrderStatus(
            anyString(), any(OrderStatus.class)
        )).thenReturn(0L);

        // Act
        RepeatStatus status = purgeTasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);

        verify(orderVersionedRepository, never())
            .deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                anyString(), anyList(), any(OrderStatus.class)
            );
    }
}
