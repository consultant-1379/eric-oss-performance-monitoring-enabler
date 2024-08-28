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

import static com.ericsson.oss.apps.util.Constants.MONITORING_OBJECT_TOPIC;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_CREATED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_FINISHED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STARTED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STOPPED_COUNT;
import static com.ericsson.oss.apps.util.Constants.VERDICT_TOPIC;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.api.model.EpmeSession;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final MonitoringObjectRepository monitoringObjectRepository;
    private final MetricService metricService;

    private static String nonNegativeHash(final String string) {
        final var nonNegativeLongHash = string.hashCode() & ((1L << 32) - 1);
        return Long.toHexString(nonNegativeLongHash);
    }

    public boolean exists(final String clientId, final String sessionReference) {
        return sessionRepository.existsByClientIdAndSessionReference(clientId, sessionReference);
    }

    public Optional<Session> findById(final String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public List<Session> findByClientId(final String clientId) {
        return sessionRepository.findAllByClientId(clientId);
    }

    public List<Session> findByClientIdAndSessionReference(final String clientId, final String sessionReference) {
        return sessionRepository.findAllByClientIdAndSessionReference(clientId, sessionReference);
    }

    public Optional<Session> findByClientIdAndSessionId(final String clientId, final String sessionId) {
        return sessionRepository.findByClientIdAndId(clientId, sessionId);
    }

    public int updateSessionToStopped(final String sessionId, final String clientId) {
        final int updateCount = sessionRepository.updateSessionToStoppedByIdAndClientId(sessionId, clientId);
        metricService.increment(PME_SESSION_STOPPED_COUNT, updateCount);
        return updateCount;
    }

    public List<Session> findAllStarted() {
        return sessionRepository.findAllByStatus(SessionStatus.STARTED);
    }

    public Session createSession(final String clientId, final EpmeSessionRequest sessionRequest, final SessionConfiguration sessionConfig) {
        final var sessionId = String.format("PME-%s-%s",
                nonNegativeHash(clientId), sessionRequest.getSessionReference());

        final var session = Session.builder()
                .id(sessionId)
                .clientId(clientId)
                .sessionReference(sessionRequest.getSessionReference())
                .duration(sessionRequest.getDuration())
                .sessionConfiguration(sessionConfig)
                .createdAt(LocalDateTime.now())
                .status(SessionStatus.CREATED)
                .build();

        final var savedSession = sessionRepository.save(session);
        metricService.increment(PME_SESSION_CREATED_COUNT);
        return savedSession;
    }

    public boolean isSessionConfigurationInUse(final long sessionConfigurationId) {
        return sessionRepository.existsBySessionConfigurationId(sessionConfigurationId);
    }

    public void updateSessionToStarted(final String sessionId) {
        final int updateCount = sessionRepository.updateSessionToStartedById(sessionId);
        metricService.increment(PME_SESSION_STARTED_COUNT, updateCount);
    }

    public void updateSessionsWithNoEnabledMonitoringObjects(final List<String> sessionIds) {
        for (final String sessionId : sessionIds) {
            log.debug("is mo stopped > 0 {}", monitoringObjectRepository.existsEnabledMonitoringObjectsByPmeSessionId(sessionId));
            if (!monitoringObjectRepository.existsEnabledMonitoringObjectsByPmeSessionId(sessionId)) {
                final int updateCount = sessionRepository.updateSessionToFinishedById(sessionId);
                metricService.increment(PME_SESSION_FINISHED_COUNT, updateCount);
            }
        }
    }

    public SessionConfiguration getConfigForSession(final String sessionId) {
        return sessionRepository.findConfigurationBySessionId(sessionId).orElse(null);
    }

    public EpmeSession dto(final Session session) {
        final var dto = new EpmeSession(
                session.getId(),
                session.getSessionReference(),
                session.getDuration(),
                Long.toString(session.getSessionConfiguration().getId()),
                MONITORING_OBJECT_TOPIC,
                VERDICT_TOPIC,
                EpmeSession.StatusEnum.valueOf(session.getStatus().name()),
                session.getCreatedAt().atOffset(ZoneOffset.UTC));

        if (Objects.nonNull(session.getStartedAt())) {
            dto.startedAt(session.getStartedAt().atOffset(ZoneOffset.UTC));
        }

        if (Objects.nonNull(session.getFinishedAt())) {
            dto.stoppedAt(session.getFinishedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }
}
