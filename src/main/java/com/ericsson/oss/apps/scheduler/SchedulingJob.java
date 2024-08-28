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
package com.ericsson.oss.apps.scheduler;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_NULL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_RETRIEVAL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_LOOKBACK_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_PA_EXECUTION_TIME_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SUCCEEDED;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.exception.KpiCalculationMonitoringHandlingException;
import com.ericsson.oss.apps.exception.KpiQueryHandlingException;
import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.service.KpiCalculationsMonitoringService;
import com.ericsson.oss.apps.service.KpiQueryService;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.MonitoringObjectService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.StartupService;
import com.ericsson.oss.apps.service.VerdictCalculationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SchedulingJob {

    private final long batchSize;
    private final SessionService sessionService;
    private final MonitoringObjectService monitoringObjectService;
    private final MonitoringObjectRepository monitoringObjectRepository;
    private final KpiQueryService kpiQueryService;
    private final VerdictCalculationService verdictCalculationService;
    private final KpiCalculationsMonitoringService kpiCalculationMonitoringService;
    @Autowired
    @Qualifier("SessionTaskExecutor")
    private Executor sessionExecutor;
    @Autowired
    private StartupService startupService;
    @Autowired
    private MetricService metricService;

    public SchedulingJob(@Value("${queryKpis.batchSize}") final long batchSize, final SessionService sessionService,
            final MonitoringObjectService monitoringObjectService,
            final MonitoringObjectRepository monitoringObjectRepository, final KpiQueryService kpiQueryService,
            final VerdictCalculationService verdictCalculationService,
            final KpiCalculationsMonitoringService kpiCalculationMonitoringService) {
        this.batchSize = batchSize;
        this.sessionService = sessionService;
        this.monitoringObjectService = monitoringObjectService;
        this.monitoringObjectRepository = monitoringObjectRepository;
        this.kpiQueryService = kpiQueryService;
        this.verdictCalculationService = verdictCalculationService;
        this.kpiCalculationMonitoringService = kpiCalculationMonitoringService;
    }

    @Scheduled(cron = "${execution.schedule.cron-expression}")
    public void executeScheduledMethod() throws InterruptedException, ControllerDetailException {
        startTimerMetrics();
        try {
            startupService.verifyServiceReady();
        } catch (final ControllerDetailException e) {
            log.info("PME Service is not ready, will not run scheduled execution");
            incrementTimerMetrics(SKIPPED);
            throw e;
        }

        log.info("Performance Monitoring Enabler Execution started");

        final var sessions = sessionService.findAllStarted();
        if (sessions.isEmpty()) {
            log.info("No monitoring sessions running");
            incrementTimerMetrics(SKIPPED);
            return;
        }

        log.info("Total number of running sessions: {}", sessions.size());

        final var sessionIds = sessions.parallelStream().map(Session::getId).toList();

        final var monitoringObjects = monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(StateEnum.ENABLED, sessionIds);

        final ZonedDateTime currentExecutionTime = Instant.now().truncatedTo(HOURS).atZone(UTC).minusHours(1);

        final var filterMonitoringObjectsForTwoHourAgo = filterMonitoringObjectsForHour(currentExecutionTime.minusHours(2L), monitoringObjects);
        final var filterMonitoringObjectsForLastHour = filterMonitoringObjectsForHour(currentExecutionTime.minusHours(1L), monitoringObjects);
        final var filterMonitoringObjectsForCurrentHour = filterMonitoringObjectsForHour(currentExecutionTime, monitoringObjects);

        try {
            executeForHour(filterMonitoringObjectsForTwoHourAgo, currentExecutionTime.minusHours(2L), false, 240);
            executeForHour(filterMonitoringObjectsForLastHour, currentExecutionTime.minusHours(1L), false, 180);
            executeForHour(filterMonitoringObjectsForCurrentHour, currentExecutionTime, true, 120);
        } catch (final InterruptedException e) {
            log.error("Error while running Performance Monitoring Enabler Execution");
            incrementTimerMetrics(FAILED);
            throw e;
        } finally {
            monitoringObjectService.updateMonitoringObjectStates(sessionIds);
            sessionService.updateSessionsWithNoEnabledMonitoringObjects(sessionIds);
        }
        incrementTimerMetrics(SUCCEEDED);
    }

    Map<String, List<MonitoringObject>> filterMonitoringObjectsForHour(final ZonedDateTime currentExecutionTime,
            final List<MonitoringObject> monitoringObjects) {
        return monitoringObjects.stream()
                .filter(mo -> isMonitoringObjectValid(currentExecutionTime, mo))
                .collect(Collectors.groupingBy(MonitoringObject::getPmeSessionId));
    }

    void executeForHour(final Map<String, List<MonitoringObject>> sessionMap,
            final ZonedDateTime executionTime,
            final Boolean retryForKpis,
            final int lookBackForKpiCalculations)
            throws InterruptedException {

        final ZonedDateTime executionTimeEnd = executionTime.plusHours(1).minusSeconds(1L);

        final String header = String.format("Execution Time: %s-%s",
                executionTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                executionTimeEnd.format(DateTimeFormatter.ofPattern("HH:mm")));

        if (sessionMap.isEmpty()) {
            log.info("{} | No monitoring objects for the current execution time.", header);
            return;
        }

        sessionMap.forEach((sessionId, monitoringObjects) -> {
            log.info("{} | Session ID: {} Monitoring Objects: {}",
                    header, sessionId, monitoringObjects.size());

            incrementMoMonitoredCountMetrics(executionTime, monitoringObjects);
        });

        final var uniqueFdns = sessionMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(MonitoringObject::getFdn)
                .collect(Collectors.toSet());
        var offset = 0L;

        if (areKpiReadyForRetrieval(header, executionTime, retryForKpis, lookBackForKpiCalculations)) {
            log.info("Starting KPI retrieval");
            while (!uniqueFdns.isEmpty()) { // NOSONAR java:S135 - multiple breaks
                final Map<String, KpiResult> batch;
                try {
                    batch = kpiQueryService.queryKpis(executionTime, batchSize, offset);
                } catch (final KpiQueryHandlingException e) {
                    log.error("{} | Failed to query KPIs", header, e);
                    break;
                }

                if (batch.isEmpty()) {
                    log.debug("{} | No KPIs retrieved for offset {}", header, offset);
                    break;
                }

                log.debug("{} | Total number of KPIs retrieved: {}", header, batch.size());

                final var filtered = kpiQueryService.filterKpis(batch, uniqueFdns);

                metricService.increment(PME_MO_KPI_RETRIEVAL_COUNT, filtered.size());

                log.info("{} | Number of filtered KPIs: {}", header, filtered.size());
                final CountDownLatch sessionLatch = new CountDownLatch(sessionMap.size());
                sessionMap.keySet()
                        .parallelStream()
                        .forEach(s -> sessionExecutor.execute(() -> {
                            verdictCalculationService.calculateVerdicts(s,
                                    filterMonitoringObjectsToBatch(sessionMap.get(s),
                                            new HashSet(filtered.keySet())),
                                    filtered, executionTime, executionTimeEnd);
                            sessionLatch.countDown();
                        }));

                sessionLatch.await();
                offset += batchSize;
            }
        }

        metricService.increment(PME_MO_KPI_NULL_COUNT, uniqueFdns.size());

        if (!uniqueFdns.isEmpty()) {
            final CountDownLatch sessionLatch = new CountDownLatch(sessionMap.size());
            sessionMap.forEach((sessionId, monitoringObjects) -> {
                final var remainingCount = monitoringObjects.stream()
                        .filter(monitoringObject -> uniqueFdns.contains(monitoringObject.getFdn())).count();
                if (remainingCount > 0) {
                    log.info("{} | Session ID: {} with remaining Monitoring Objects: {}", header, sessionId, remainingCount);
                    sessionExecutor.execute(() -> {
                        verdictCalculationService.calculateVerdicts(sessionId, monitoringObjects, new HashMap<>(),
                                executionTime, executionTimeEnd);
                        sessionLatch.countDown();
                    });
                } else {
                    sessionLatch.countDown();
                }
            });
            sessionLatch.await();
        }

        log.info("Finished hourly execution for {}", header);
    }

    private List<MonitoringObject> filterMonitoringObjectsToBatch(final List<MonitoringObject> monitoringObjects,
            final Collection<String> fdnsInBatch) {
        return monitoringObjects
                .parallelStream()
                .filter(mo -> fdnsInBatch.remove(mo.getFdn()))
                .collect(Collectors.toList());
    }

    private boolean areKpiReadyForRetrieval(final String header, final ZonedDateTime executionTime,
            final Boolean retryForKpis, final int lookBackForKpiCalculations) throws InterruptedException {
        List<ZonedDateTime> readinessResults = new ArrayList<>();

        try {
            log.info("{} | Retrieving KPI calculation readiness status for execution hour", header);
            readinessResults = kpiCalculationMonitoringService.checkKpiReadiness(executionTime, retryForKpis, lookBackForKpiCalculations);
        } catch (final KpiCalculationMonitoringHandlingException e) {
            log.error("Failed to get KPI Calculation status", e);
        }

        if (readinessResults.isEmpty()) {
            log.info("There are no calculated KPIs available for execution hour");
            return false;
        }
        readinessResults.forEach(rop -> log.info("KPIs calculated for: {} rop", rop));
        return true;
    }

    private boolean isMonitoringObjectValid(final ZonedDateTime currentExecutionTime, final MonitoringObject monitoringObject) {
        // This filters out monitoring objects that have already ended.
        if (monitoringObject.getEndTime().isBefore(currentExecutionTime)) {
            return false;
            // This filters out monitoring objects outside monitoring window or started at the same time as the current execution.
        } else if (monitoringObject.getLastProcessedTime() == null) {
            return monitoringObject.getStartTime().isBefore(currentExecutionTime) ||
                    monitoringObject.getStartTime().equals(currentExecutionTime);
            // This filters out monitoring objects that are processed after the current execution time.
        } else {
            return monitoringObject.getLastProcessedTime().isBefore(currentExecutionTime);
        }
    }

    private void incrementMoMonitoredCountMetrics(final ZonedDateTime executionTime, final List<MonitoringObject> monitoringObjects) {
        final ZonedDateTime currentExecutionTime = Instant.now().truncatedTo(HOURS).atZone(UTC).minusHours(1);

        if (executionTime.isBefore(currentExecutionTime)) {
            metricService.increment(PME_MO_MONITORED_LOOKBACK_COUNT, monitoringObjects.size());
        } else {
            metricService.increment(PME_MO_MONITORED_COUNT, monitoringObjects.size());
        }
    }

    private void startTimerMetrics() {
        metricService.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, FAILED);
        metricService.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        metricService.startTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SKIPPED);
    }

    private void incrementTimerMetrics(final String status) {
        metricService.stopTimer(String.valueOf(hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, status);
        metricService.increment(PME_PA_EXECUTION_TIME_HOURLY, STATUS, status);
    }
}