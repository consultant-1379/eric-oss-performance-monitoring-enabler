/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import com.ericsson.oss.apps.exception.KafkaStartupHandlingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.exception.DataSubscriptionHandlingException;

@ExtendWith(MockitoExtension.class)
class StartupServiceTest {
    @Mock
    private DataSubscriptionService dataSubscriptionService;
    @Mock
    private KpiDefinitionsService kpiDefinitionsService;
    @Mock
    private KafkaStartupService kafkaStartupService;
    @Mock
    private ThreadPoolTaskScheduler scheduler;

    private StartupService objectUnderTest;

    @BeforeEach
    void setUp() {
        objectUnderTest = new StartupService(dataSubscriptionService, kpiDefinitionsService, kafkaStartupService,
                scheduler, 30L);
    }

    @Test
    void whenScheduleStartUpOperations_verifyMethodIsScheduled() {
        objectUnderTest.scheduleStartUpOperations();

        verify(scheduler, times(1))
                .schedule(eq(objectUnderTest.runnable), any(Instant.class));
    }

    @Test
    void whenRunStartupOperations_servicesAreCalledInOrder() {
        final var inOrder = inOrder(dataSubscriptionService, kpiDefinitionsService, kafkaStartupService);
        assertThat(objectUnderTest.getStatus()).isEqualTo(StartupService.StartupStatus.STARTED);

        objectUnderTest.runStartupOperations();

        inOrder.verify(dataSubscriptionService, times(1)).checkActiveSubscriptions();
        inOrder.verify(kpiDefinitionsService, times(1)).postKpiDefinitions();
        inOrder.verify(kafkaStartupService, times(1)).startSecureKafka();
        inOrder.verifyNoMoreInteractions();

        assertThat(objectUnderTest.getStatus()).isEqualTo(StartupService.StartupStatus.READY);
    }

    @Test
    void whenRunStartupOperations_andCheckActiveDataSubscriptionsFails_thenStatusIsFailed() {
        doThrow(DataSubscriptionHandlingException.class).when(dataSubscriptionService).checkActiveSubscriptions();
        final var inOrder = inOrder(dataSubscriptionService, kpiDefinitionsService);
        objectUnderTest.runStartupOperations();

        inOrder.verify(dataSubscriptionService, times(1)).checkActiveSubscriptions();
        inOrder.verify(kpiDefinitionsService, never()).postKpiDefinitions();
        inOrder.verifyNoMoreInteractions();

        assertThat(objectUnderTest.getStatus()).isEqualTo(StartupService.StartupStatus.FAILED);
    }

    @Test
    void whenRunStartupOperations_andPostKpiDefinitionsFails_thenStatusIsFailed() {
        doThrow(DataSubscriptionHandlingException.class).when(kpiDefinitionsService).postKpiDefinitions();
        final var inOrder = inOrder(dataSubscriptionService, kpiDefinitionsService);
        objectUnderTest.runStartupOperations();

        inOrder.verify(dataSubscriptionService, times(1)).checkActiveSubscriptions();
        inOrder.verify(kpiDefinitionsService, times(1)).postKpiDefinitions();
        inOrder.verifyNoMoreInteractions();

        assertThat(objectUnderTest.getStatus()).isEqualTo(StartupService.StartupStatus.FAILED);
    }

    @Test
    void whenRunStartupOperations_andStartSecureKafkaFails_thenStatusIsFailed() {
        doThrow(KafkaStartupHandlingException.class).when(kafkaStartupService).startSecureKafka();
        final var inOrder = inOrder(dataSubscriptionService, kpiDefinitionsService);
        objectUnderTest.runStartupOperations();

        inOrder.verify(dataSubscriptionService, times(1)).checkActiveSubscriptions();
        inOrder.verify(kpiDefinitionsService, times(1)).postKpiDefinitions();
        inOrder.verifyNoMoreInteractions();

        assertThat(objectUnderTest.getStatus()).isEqualTo(StartupService.StartupStatus.FAILED);
    }

    @Test
    void whenVerifyServiceReady_andStartupStatusIsReady_thenNoExceptionIsThrown() {
        objectUnderTest.runStartupOperations();

        assertThatCode(() -> objectUnderTest.verifyServiceReady())
                .doesNotThrowAnyException();
    }

    @Test
    void whenVerifyServiceReady_andStartupStatusIsStarted_thenExceptionIsThrown() {
        assertThatThrownBy(() -> objectUnderTest.verifyServiceReady())
                .isInstanceOf(ControllerDetailException.class)
                .extracting("status", "reason", "detail")
                .containsExactly(HttpStatus.SERVICE_UNAVAILABLE, "Service is not ready", "Start up operations have started");
    }

    @Test
    void whenVerifyServiceReady_andStartupStatusIsFailed_thenExceptionIsThrown() {
        doThrow(DataSubscriptionHandlingException.class).when(kpiDefinitionsService).postKpiDefinitions();
        objectUnderTest.runStartupOperations();

        assertThatThrownBy(() -> objectUnderTest.verifyServiceReady())
                .isInstanceOf(ControllerDetailException.class)
                .extracting("status", "reason", "detail")
                .containsExactly(HttpStatus.SERVICE_UNAVAILABLE, "Service is not ready", "Start up operations failed");
    }
}