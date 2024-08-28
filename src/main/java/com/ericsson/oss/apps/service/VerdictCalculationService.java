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
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.kafka.VerdictKafkaProducer;
import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.util.VerdictCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerdictCalculationService {
    private final MonitoringObjectService monitoringObjectService;
    private final SessionService sessionService;
    private final VerdictKafkaProducer verdictKafkaProducer;
    private final MetricService metricService;

    public void calculateVerdicts(final String sessionId, final List<MonitoringObject> monitoringObjects,
            final Map<String, KpiResult> kpis, final ZonedDateTime executionStartTime,
            final ZonedDateTime executionEndTime) {
        log.info("Calculating verdicts for Session id '{}'. {} monitoring objects will be processed on this iteration. " +
                "{} KPIs are in the current batch",
                sessionId, monitoringObjects.size(), kpis.size());

        final var configuration = sessionService.getConfigForSession(sessionId);
        final var calculator = new VerdictCalculator(sessionId, executionStartTime, configuration, kpis, metricService);

        monitoringObjects.parallelStream()
                .map(MonitoringObject::getFdn)
                .map(calculator::createVerdict)
                .forEach(verdictKafkaProducer::sendVerdict);

        monitoringObjectService.updateLastProcessedTimes(monitoringObjects, executionEndTime);
    }
}
