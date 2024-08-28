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

import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_PA_EXECUTION_TIME_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_CREATED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
public class MetricServiceTest {

    private MeterRegistry meterRegistry;
    private MetricService objectUnderTest;

    @BeforeEach
    public void setup() {
        meterRegistry = new SimpleMeterRegistry();
        objectUnderTest = new MetricService(meterRegistry);
    }

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void whenCounterMetricIsIncrementedInitially_thenCounterMetricIsCreatedAndIncremented() {
        objectUnderTest.increment(PME_SESSION_CREATED_COUNT);
        assertThat(objectUnderTest.findCounter(PME_SESSION_CREATED_COUNT).get().count()).isEqualTo(1.0);
    }

    @Test
    void whenCounterMetricWhichAlreadyExistsIsIncremented_thenCounterMetricIsFoundIncremented() {
        objectUnderTest.increment(PME_SESSION_CREATED_COUNT);
        objectUnderTest.increment(PME_SESSION_CREATED_COUNT);
        assertThat(objectUnderTest.findCounter(PME_SESSION_CREATED_COUNT).get().count()).isEqualTo(2.0);
    }

    @Test
    void whenCounterMetricIsIncrementedWithValue_thenCounterMetricIsIncrementedWithThatValue() {
        objectUnderTest.increment(PME_SESSION_CREATED_COUNT, 5);
        assertThat(objectUnderTest.findCounter(PME_SESSION_CREATED_COUNT).get().count()).isEqualTo(5.0);
    }

    @Test
    void whenFindCounterMetricCalledForNonExistingCounter_thenEmptyOptionalIsReturned() {
        assertThat(objectUnderTest.findCounter(PME_SESSION_CREATED_COUNT)).isEmpty();
    }

    @Test
    void whenTimerMetricIsStarted_thenTimerMetricIsCreatedAndTimerIsZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        assertThat(objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED).get().totalTime(TimeUnit.MILLISECONDS))
                .isZero();
    }

    @Test
    void whenTimerMetricIsStartedAndStoppedInitially_thenTimerMetricIsCreatedAndTimerIsGreaterThanZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        assertThat(objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED).get().totalTime(TimeUnit.MILLISECONDS))
                .isGreaterThan(0.0);
    }

    @Test
    void whenTimerMetricIsStartedAndStoppedWhichAlreadyExists_thenTimerMetricIsFoundAndTimerIsGreaterThanTheInitialTime() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        final double timeAfterFirstStartStop = objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED).get()
                .totalTime(TimeUnit.MILLISECONDS);
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        objectUnderTest.stopTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        assertThat(objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED).get().totalTime(TimeUnit.MILLISECONDS))
                .isGreaterThan(timeAfterFirstStartStop);
    }

    @Test
    void whenTimerMetricIsStartedTwice_thenTimerIsStillZero() {
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        objectUnderTest.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        assertThat(objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED).get().totalTime(TimeUnit.MILLISECONDS))
                .isZero();
    }

    @Test
    void whenFindTimerMetricCalledForNonExistingCounter_thenEmptyOptionalIsReturned() {
        assertThat(objectUnderTest.findTimer(PME_PA_EXECUTION_TIME_HOURLY)).isEmpty();
    }

    @Test
    void whenMetricsAreInitialized_thenVerifyCountTotal() {
        objectUnderTest.initializeMetrics();
        assertThat(meterRegistry.getMeters()).hasSize(67);
    }

}
