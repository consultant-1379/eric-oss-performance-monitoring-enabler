/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_DISCARDED_COUNT;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringObjectKafkaService {

    private final MonitoringObjectRepository monitoringObjectRepository;
    private final MetricService metricService;

    private final SessionService sessionService;

    public void consume(final MonitoringObjectMessage message) {
        log.debug("Consumed MonitoringObjectMessage from kafka topic: '{}'", message);

        final var monitoringObject = new MonitoringObject(message);

        final var sessionOptional = sessionService.findById(monitoringObject.getPmeSessionId());

        if (sessionOptional.isEmpty()) {
            log.debug("Monitoring Object received with PME session ID that does not exist, ignoring Monitoring Object");
            metricService.increment(PME_MO_DISCARDED_COUNT, 1);
            return;
        }

        final var session = sessionOptional.get();
        if (SessionStatus.STOPPED.equals(session.getStatus()) || SessionStatus.FINISHED.equals(session.getStatus())) {
            log.debug("Monitoring Object received for a completed PME session, ignoring Monitoring Object");
            metricService.increment(PME_MO_DISCARDED_COUNT, 1);
            return;
        }

        metricService.increment(PME_MO_COUNT, 1);

        monitoringObject.setEndTime(
                monitoringObject.getStartTime().plusHours(session.getDuration()).minusSeconds(1L));

        monitoringObjectRepository.save(monitoringObject);

        if (StateEnum.ENABLED.equals(monitoringObject.getState())) {
            sessionService.updateSessionToStarted(monitoringObject.getPmeSessionId());
        }
    }
}
