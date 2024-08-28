/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.exception.ControllerDetailException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StartupService {
    // Lambda reference stored for test assertions
    final Runnable runnable = this::runStartupOperations;
    private final DataSubscriptionService dataSubscriptionService;
    private final KpiDefinitionsService kpiDefinitionsService;
    private final KafkaStartupService kafkaStartupService;
    private final ThreadPoolTaskScheduler scheduler;
    private final Long initialDelayInSeconds;
    private final AtomicReference<StartupStatus> startupStatus = new AtomicReference<>(StartupStatus.STARTED);

    public StartupService(final DataSubscriptionService dataSubscriptionService,
            final KpiDefinitionsService kpiDefinitionsService, final KafkaStartupService kafkaStartupService,
            final ThreadPoolTaskScheduler scheduler,
            @Value("${startup.initialDelayInSeconds}") final Long initialDelayInSeconds) {
        this.dataSubscriptionService = dataSubscriptionService;
        this.kpiDefinitionsService = kpiDefinitionsService;
        this.kafkaStartupService = kafkaStartupService;
        this.scheduler = scheduler;
        this.initialDelayInSeconds = initialDelayInSeconds;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleStartUpOperations() {
        startupStatus.set(StartupStatus.STARTED);
        scheduler.schedule(runnable, Instant.now().plusSeconds(initialDelayInSeconds));
    }

    public void verifyServiceReady() {
        if (!StartupStatus.READY.equals(startupStatus.get())) {
            throw ControllerDetailException.builder()
                    .withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                    .withReason("Service is not ready")
                    .withDetail(startupStatus.get().description)
                    .build();
        }
    }

    void runStartupOperations() {
        try {
            log.info("Running Start up Operations");

            startupStatus.set(StartupStatus.VALIDATING_DATA_SUBSCRIPTIONS);
            dataSubscriptionService.checkActiveSubscriptions();

            startupStatus.set(StartupStatus.SENDING_KPI_DEFINITIONS);
            kpiDefinitionsService.postKpiDefinitions();

            startupStatus.set(StartupStatus.STARTING_SECURE_KAFKA);
            kafkaStartupService.startSecureKafka();

            startupStatus.set(StartupStatus.READY);
            log.info("Start up Operations complete");
        } catch (final Exception e) {
            startupStatus.set(StartupStatus.FAILED);
            log.error("Start up Operations failed", e);
        }
    }

    public StartupStatus getStatus() {
        return startupStatus.get();
    }

    @Getter
    @RequiredArgsConstructor
    public enum StartupStatus {
        STARTED("STARTED", "Start up operations have started"),
        VALIDATING_DATA_SUBSCRIPTIONS("RUNNING", "Validating PME Data Subscriptions"),
        SENDING_KPI_DEFINITIONS("RUNNING", "Sending PME KPI Definitions"),
        STARTING_SECURE_KAFKA("RUNNING", "Starting Secure Kafka Communication"),
        READY("READY", "Start up operations completed successfully"),
        FAILED(Status.OUT_OF_SERVICE.getCode(), "Start up operations failed");

        private final String status;
        private final String description;
    }
}
