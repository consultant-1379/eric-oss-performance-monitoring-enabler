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

import static com.ericsson.assertions.response.ResponseEntityAssertions.assertThat;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DURATION;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_CONFIGURATION;
import static com.ericsson.oss.apps.util.TestConstants.WEEKEND_DAYS;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;
import com.ericsson.oss.apps.repository.SessionRepository;

import jakarta.annotation.PostConstruct;

@DirtiesContext
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RetentionServiceTest {

    private final LocalDateTime currentTime = LocalDateTime.now();
    private final SessionRepository sessionRepository;
    private final MonitoringObjectRepository moRepository;
    private final RetentionService objectUnderTest;

    private final SessionConfigurationRepository sessionConfigurationRepository;

    @Autowired
    public RetentionServiceTest(final SessionRepository sessionRepository, final MonitoringObjectRepository moRepository,
            final RetentionService objectUnderTest, final SessionConfigurationRepository sessionConfigurationRepository) {
        this.sessionRepository = sessionRepository;
        this.moRepository = moRepository;
        this.objectUnderTest = objectUnderTest;
        this.sessionConfigurationRepository = sessionConfigurationRepository;
    }

    @PostConstruct
    public void createSessionConfiguration() {
        sessionConfigurationRepository.save(buildSessionConfiguration());
    }

    @BeforeEach
    public void clearDatabase() {
        sessionRepository.deleteAll();
        moRepository.deleteAll();
    }

    @Test
    public void whenTwoStoppedSessionsExist_andOnlyOneIsExpired_theUnexpiredSessionRemains() {
        sessionRepository.save(createSession("expiredStoppedSession", "expiredStoppedSession",
                currentTime.minusHours(67), currentTime.minusHours(49), SessionStatus.STOPPED));
        sessionRepository.save(createSession("unexpiredStoppedSession", "unexpiredStoppedSession",
                currentTime.minusHours(65), currentTime.minusHours(47), SessionStatus.STOPPED));
        moRepository.save(createMonitoringObject("expiredStoppedSession_fdn_1", "expiredStoppedSession"));
        moRepository.save(createMonitoringObject("expiredStoppedSession_fdn_2", "expiredStoppedSession"));
        moRepository.save(createMonitoringObject("unexpiredStoppedSession_fdn_1", "unexpiredStoppedSession"));
        moRepository.save(createMonitoringObject("unexpiredStoppedSession_fdn_2", "unexpiredStoppedSession"));
        objectUnderTest.removeExpiredSessions();
        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(moRepository.count()).isEqualTo(2);
    }

    @Test
    public void whenASessionIsInStateCreated_andItsStartedTimeHasExpired_itIsRemovedRegardlessOfItsFinishedTime() {
        sessionRepository.save(createSession("createdSession", "createdSession",
                currentTime.minusHours(49), currentTime, SessionStatus.CREATED));
        moRepository.save(createMonitoringObject("createdSession_fdn_1", "createdSession"));
        moRepository.save(createMonitoringObject("createdSession_fdn_2", "createdSession"));
        objectUnderTest.removeExpiredSessions();
        assertThat(sessionRepository.count()).isEqualTo(0);
        assertThat(moRepository.count()).isEqualTo(0);
    }

    @Test
    public void whenAnExpiredSessionExists_andItsStateIsNotStarted_itIsRemoved() {
        addAllSessions();
        addAllMonitoringObjects();
        objectUnderTest.removeExpiredSessions();
        assertThat(sessionRepository.count()).isEqualTo(2);
        assertThat(moRepository.count()).isEqualTo(4);
    }

    private void addAllSessions() {
        sessionRepository.save(createSession("expiredStoppedSession", "expiredStoppedSession",
                currentTime.minusHours(67), currentTime.minusHours(49), SessionStatus.STOPPED));
        sessionRepository.save(createSession("unexpiredStoppedSession", "unexpiredStoppedSession",
                currentTime.minusHours(65), currentTime.minusHours(47), SessionStatus.STOPPED));
        sessionRepository.save(createSession("finishedSession", "finishedSession",
                currentTime.minusHours(67), currentTime.minusHours(49), SessionStatus.FINISHED));
        sessionRepository.save(createSession("createdSession", "createdSession",
                currentTime.minusHours(49), currentTime, SessionStatus.CREATED));
        sessionRepository.save(createSession("startedSession", "startedSession",
                currentTime.minusHours(100), currentTime.minusHours(100), SessionStatus.STARTED));
    }

    private void addAllMonitoringObjects() {
        moRepository.save(createMonitoringObject("expiredStoppedSession_fdn_1", "expiredStoppedSession"));
        moRepository.save(createMonitoringObject("expiredStoppedSession_fdn_2", "expiredStoppedSession"));
        moRepository.save(createMonitoringObject("unexpiredStoppedSession_fdn_1", "unexpiredStoppedSession"));
        moRepository.save(createMonitoringObject("unexpiredStoppedSession_fdn_2", "unexpiredStoppedSession"));
        moRepository.save(createMonitoringObject("finishedSession_fdn_1", "finishedSession"));
        moRepository.save(createMonitoringObject("finishedSession_fdn_2", "finishedSession"));
        moRepository.save(createMonitoringObject("createdSession_fdn_1", "createdSession"));
        moRepository.save(createMonitoringObject("createdSession_fdn_2", "createdSession"));
        moRepository.save(createMonitoringObject("startedSession_fdn_1", "startedSession"));
        moRepository.save(createMonitoringObject("startedSession_fdn_2", "startedSession"));
    }

    private Session createSession(final String id, final String sessionReference, final LocalDateTime createdAt,
            final LocalDateTime finishedAt, final SessionStatus status) {
        final SessionConfiguration config = new SessionConfiguration();
        config.setId(1L);
        final Session session = Session.builder()
                .id(id)
                .clientId(CLIENT_APP_ID)
                .sessionReference(sessionReference)
                .duration(DURATION)
                .sessionConfiguration(config)
                .createdAt(createdAt)
                .startedAt(createdAt)
                .finishedAt(finishedAt)
                .status(status)
                .build();
        return session;
    }

    public MonitoringObject createMonitoringObject(final String fdn, final String pmeSessionId) {
        final MonitoringObject mo = MonitoringObject.builder()
                .fdn(fdn)
                .pmeSessionId(pmeSessionId)
                .state(StateEnum.ENABLED)
                .lastProcessedTime(ZonedDateTime.now())
                .startTime(ZonedDateTime.now())
                .build();
        return mo;
    }

    private SessionConfiguration buildSessionConfiguration() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        sessionConfiguration.setId(PME_CONFIGURATION_ID);
        sessionConfiguration.setName(SESSION_CONFIGURATION);
        sessionConfiguration.setWeekendDays(WEEKEND_DAYS);
        return sessionConfiguration;
    }
}