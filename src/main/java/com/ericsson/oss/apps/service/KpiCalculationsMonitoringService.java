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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsc.MonitorKpiCalculationsApi;
import com.ericsson.oss.apps.client.pmsc.model.CalculationGetResponseInner;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponse;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponseReadinessLogsInner;
import com.ericsson.oss.apps.exception.KpiCalculationMonitoringHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KpiCalculationsMonitoringService {

    private static final int REQUIRED_NUM_ROPS = 4;

    private final MonitorKpiCalculationsApi monitorKpiCalculationsApi;

    private final int numberOfReadinessRetries;

    private final int readinessRetriesBackoff;

    private final AtomicInteger numberOfExecutedRetries = new AtomicInteger(0);

    public KpiCalculationsMonitoringService(final MonitorKpiCalculationsApi monitorKpiCalculationsApi,
            @Value("${kpiCalculationMonitoring.kpiReadinessRetry.numberOfRetries}") final int numberOfReadinessRetries,
            @Value("${kpiCalculationMonitoring.kpiReadinessRetry.backoff}") final int readinessRetriesBackoff) {
        this.monitorKpiCalculationsApi = monitorKpiCalculationsApi;
        this.numberOfReadinessRetries = numberOfReadinessRetries;
        this.readinessRetriesBackoff = readinessRetriesBackoff;
    }

    List<CalculationGetResponseInner> getKpiCalculationsForElapsedTime(final int lookBackForKpiCalculations) {
        final var elapsedMinutes = Integer.valueOf(lookBackForKpiCalculations);
        final List<CalculationGetResponseInner> kpiCalculations;
        try {
            kpiCalculations = monitorKpiCalculationsApi.findCalculationsCreatedAfter(elapsedMinutes, false);
        } catch (final ResourceAccessException | HttpServerErrorException | TokenAuthenticationException e) {
            log.error("Failed to get Kpi Calculations", e);
            throw new KpiCalculationMonitoringHandlingException("Failed to get Kpi Calculations", e);
        }
        return kpiCalculations;
    }

    CalculationResponse getKpiCalculationById(final String calculationId) {
        final CalculationResponse calculationResponse;
        try {
            calculationResponse = monitorKpiCalculationsApi.getApplicationState(UUID.fromString(calculationId));
        } catch (final ResourceAccessException | HttpServerErrorException | TokenAuthenticationException e) {
            log.error("Failed to get KPI Calculations", e);
            throw new KpiCalculationMonitoringHandlingException("Failed to get KPI Calculations", e);
        }
        return calculationResponse;
    }

    @Retryable(retryFor = {
            KpiCalculationMonitoringHandlingException.class },
            maxAttemptsExpression = "${kpiCalculationMonitoring.retry.get.maxAttempts}",
            backoff = @Backoff(delayExpression = "${kpiCalculationMonitoring.retry.get.backoff}"))
    public List<ZonedDateTime> checkKpiReadiness(final ZonedDateTime executionTime,
            final Boolean retryForKpis, final int lookBackForKpiCalculations) throws InterruptedException {
        log.info("Retrieving KPI calculation readiness status for execution hour starting: {}", executionTime);
        final var requiredRops = Arrays.asList(executionTime, executionTime.plusMinutes(15), executionTime.plusMinutes(30),
                executionTime.plusMinutes(45));
        final List<ZonedDateTime> ropsInReadinessLog = new ArrayList<>();
        while (ropsInReadinessLog.size() != REQUIRED_NUM_ROPS && numberOfExecutedRetries.get() < numberOfReadinessRetries) {
            ropsInReadinessLog.clear();
            final var allKpiCalculationsForElapsedTime = getKpiCalculationsForElapsedTime(lookBackForKpiCalculations);

            final var complexKpiCalculations = allKpiCalculationsForElapsedTime.stream()
                    .filter(kpisCalculation -> kpisCalculation.getKpiType().equals("SCHEDULED_COMPLEX")
                            && kpisCalculation.getStatus().equals("FINISHED"))
                    .toList();

            final ArrayList allComplexKpiReadinessLogs = new ArrayList();
            complexKpiCalculations.forEach(complexKpiCalculation -> allComplexKpiReadinessLogs
                    .addAll(getKpiCalculationById(complexKpiCalculation.getCalculationId()).getReadinessLogs()));

            for (final ZonedDateTime requiredRop : requiredRops) {
                if (readinessLogContainsRequiredRop(requiredRop, allComplexKpiReadinessLogs)) {
                    ropsInReadinessLog.add(requiredRop);
                }
            }
            if (ropsInReadinessLog.size() != REQUIRED_NUM_ROPS) {
                if (retryForKpis && numberOfExecutedRetries.getAndIncrement() < numberOfReadinessRetries) {
                    log.info("Not all complex KPIs are calculated for current hour, will retry: retry number: {}", numberOfExecutedRetries);
                    Thread.sleep(readinessRetriesBackoff);
                } else {
                    log.info("Not all complex KPIs are calculated for current hour");
                    break;
                }
            }
        }
        numberOfExecutedRetries.set(0);
        return ropsInReadinessLog;
    }

    private boolean readinessLogContainsRequiredRop(final ZonedDateTime requiredRop,
            final List<CalculationResponseReadinessLogsInner> readinessLogs) {
        if (!readinessLogs.isEmpty()) {
            for (final CalculationResponseReadinessLogsInner readinessLog : readinessLogs) {
                final ZonedDateTime earliestCollectedData = ZonedDateTime.parse(readinessLog.getEarliestCollectedData() + "z");
                final ZonedDateTime latestCollectedData = ZonedDateTime.parse(readinessLog.getLatestCollectedData() + "z");
                if (requiredRop.equals(latestCollectedData) || requiredRop.equals(earliestCollectedData)
                        || (requiredRop.isAfter(earliestCollectedData) && (requiredRop.isBefore(latestCollectedData)))) {
                    log.debug("found a ROP for {} and readinessLog datasource {} - ROP is start time {} and end time {} ", requiredRop,
                            readinessLog.getDatasource(), earliestCollectedData, latestCollectedData);
                    return true;
                }
            }
        }
        return false;
    }
}