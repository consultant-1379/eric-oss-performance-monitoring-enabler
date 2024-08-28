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

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringObjectService {

    private final MonitoringObjectRepository monitoringObjectRepository;

    public void updateLastProcessedTimes(final List<MonitoringObject> monitoringObjects, final ZonedDateTime lastProcessedTime) {
        log.info("Updating last processed time of {} Monitoring Objects", monitoringObjects.size());
        for (final MonitoringObject monitoringObject : monitoringObjects) {
            monitoringObject.setLastProcessedTime(lastProcessedTime);
            monitoringObjectRepository.save(monitoringObject);
        }
    }

    public void updateMonitoringObjectStates(final List<String> sessionIds) {
        for (final String sessionId : sessionIds) {
            log.info("Updating status of {} Session", sessionId);
            monitoringObjectRepository.updateMonitoringObjectStateByPmeSessionId(sessionId);
        }
    }
}