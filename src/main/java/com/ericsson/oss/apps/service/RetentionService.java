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

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class RetentionService {

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    MonitoringObjectRepository moRepository;

    @Value("${database.retention.sessionExpirationPeriodInHours}")
    int expirationPeriodInHours;

    @Scheduled(cron = "${database.retention.sessionCronExpression}")
    public void removeExpiredSessions() {
        log.info(String.format("Starting removal of Sessions older than %s hours, " +
                "Monitoring Objects associated with expired Sessions will also be removed", expirationPeriodInHours));
        clearSessionsByCreatedAtTimestamp(SessionStatus.CREATED);
        clearSessionsByFinishedAtTimestamp(SessionStatus.STOPPED);
        clearSessionsByFinishedAtTimestamp(SessionStatus.FINISHED);
    }

    private void clearSessionsByCreatedAtTimestamp(final SessionStatus status) {
        final List<Session> sessions = sessionRepository.findByStatusAndCreatedAtBefore(
                status, LocalDateTime.now().minusHours(expirationPeriodInHours));
        for (final Session session: sessions) {
            moRepository.deleteByPmeSessionId(session.getId());
            sessionRepository.delete(session);
        }
    }

   private void clearSessionsByFinishedAtTimestamp(final SessionStatus status) {
       final List<Session> sessions = sessionRepository.findByStatusAndFinishedAtBefore(
               status, LocalDateTime.now().minusHours(expirationPeriodInHours));
       for (final Session session: sessions) {
           moRepository.deleteByPmeSessionId(session.getId());
           sessionRepository.delete(session);
       }
    }
}
