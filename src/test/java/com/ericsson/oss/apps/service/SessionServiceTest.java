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

import static com.ericsson.oss.apps.util.Constants.CLIENT_ID;
import static com.ericsson.oss.apps.util.Constants.MONITORING_OBJECT_TOPIC;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_CREATED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_FINISHED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STARTED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STOPPED_COUNT;
import static com.ericsson.oss.apps.util.Constants.VERDICT_TOPIC;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DURATION;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID_2;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID_3;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.api.model.EpmeSession;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MonitoringObjectRepository monitoringObjectRepository;

    @Mock
    private MetricService mockMetricService;

    @InjectMocks
    private SessionService objectUnderTest;

    @Test
    void whenCheckSessionExists_andExistsInRepository_thenTrueIsReturned() {
        when(sessionRepository.existsByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE))
                .thenReturn(true);

        assertThat(objectUnderTest.exists(CLIENT_APP_ID, SESSION_REFERENCE)).isTrue();
    }

    @Test
    void whenCheckSessionExists_andDoesNotExistInRepository_thenFalseIsReturned() {
        when(sessionRepository.existsByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE))
                .thenReturn(false);

        assertThat(objectUnderTest.exists(CLIENT_APP_ID, SESSION_REFERENCE)).isFalse();
    }

    @Test
    void whenUpdateSessionsWithNoEnabledMonitoringObjects_verifySessionsUpdatedCalled() {
        final List<String> sessionIds = Arrays.asList(PME_SESSION_ID, PME_SESSION_ID_2, PME_SESSION_ID_3);

        when(monitoringObjectRepository.existsEnabledMonitoringObjectsByPmeSessionId(PME_SESSION_ID)).thenReturn(true);
        when(monitoringObjectRepository.existsEnabledMonitoringObjectsByPmeSessionId(PME_SESSION_ID_2)).thenReturn(false);
        when(monitoringObjectRepository.existsEnabledMonitoringObjectsByPmeSessionId(PME_SESSION_ID_3)).thenReturn(true);
        when(sessionRepository.updateSessionToFinishedById(PME_SESSION_ID_2)).thenReturn(1);

        objectUnderTest.updateSessionsWithNoEnabledMonitoringObjects(sessionIds);

        verify(sessionRepository, times(1)).updateSessionToFinishedById(PME_SESSION_ID_2);
        verify(sessionRepository, never()).updateSessionToFinishedById(PME_SESSION_ID);
        verify(sessionRepository, never()).updateSessionToFinishedById(PME_SESSION_ID_3);
        verify(mockMetricService, times(1)).increment(PME_SESSION_FINISHED_COUNT, 1);
    }

    @Test
    void whenCreateSession_verifySessionRepositorySaveIsCalled() {
        final var sessionConfig = mock(SessionConfiguration.class);
        when(sessionConfig.getId()).thenReturn(PME_CONFIGURATION_ID);
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));

        final var captor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.save(captor.capture()))
                .thenReturn(null);

        objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, sessionConfig);

        assertThat(captor.getValue())
                .extracting("id", "clientId", "sessionReference", "duration",
                        "sessionConfiguration.id", "status")
                .containsExactly(PME_SESSION_ID, CLIENT_APP_ID, SESSION_REFERENCE, DURATION,
                        PME_CONFIGURATION_ID, SessionStatus.CREATED);

        verify(sessionRepository, times(1)).save(captor.getValue());
        verify(mockMetricService, times(1)).increment(PME_SESSION_CREATED_COUNT);
    }

    @Test
    void whenGetDtoForSession_verifyCorrectValuesReturned() {
        final var createdAt = LocalDateTime.of(2023, 10, 24, 15, 0, 0);
        final var startedAt = LocalDateTime.of(2023, 10, 24, 15, 30, 0);

        final var sessionConfig = mock(SessionConfiguration.class);
        when(sessionConfig.getId()).thenReturn(PME_CONFIGURATION_ID);

        final var session = Session.builder()
                .id(PME_SESSION_ID)
                .clientId(CLIENT_APP_ID)
                .sessionReference(SESSION_REFERENCE)
                .duration(DURATION)
                .sessionConfiguration(sessionConfig)
                .createdAt(createdAt)
                .startedAt(startedAt)
                .status(SessionStatus.STARTED)
                .build();

        final var sessionDto = objectUnderTest.dto(session);

        assertThat(sessionDto)
                .extracting("id", "sessionReference", "duration", "pmeConfigId",
                        "monitoringObjectTopicName", "verdictTopicName", "status",
                        "createdAt", "startedAt")
                .containsExactly(PME_SESSION_ID, SESSION_REFERENCE, DURATION, Long.toString(PME_CONFIGURATION_ID),
                        MONITORING_OBJECT_TOPIC, VERDICT_TOPIC, EpmeSession.StatusEnum.STARTED,
                        createdAt.atOffset(ZoneOffset.UTC), startedAt.atOffset(ZoneOffset.UTC));
    }

    @Test
    void whenSetSessionToStart_andSessionIsStatusCreated_thenSaveIsCalled() {
        when(sessionRepository.updateSessionToStartedById(PME_SESSION_ID)).thenReturn(1);
        objectUnderTest.updateSessionToStarted(PME_SESSION_ID);
        verify(sessionRepository, times(1)).updateSessionToStartedById(PME_SESSION_ID);
        verifyNoMoreInteractions(sessionRepository);
        verify(mockMetricService, times(1)).increment(PME_SESSION_STARTED_COUNT, 1);
    }

    @Test
    public void whenCheckingIsConfigurationInUse_andIsAssociatedWithASession_thenReturnTrue() {
        when(sessionRepository.existsBySessionConfigurationId(anyLong())).thenReturn(true);
        assertThat(objectUnderTest.isSessionConfigurationInUse(1l)).isTrue();
        verify(sessionRepository, times(1)).existsBySessionConfigurationId(anyLong());
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    public void whenCheckingIsConfigurationInUse_andIsAssociatedWithNoSession_thenReturnFalse() {
        when(sessionRepository.existsBySessionConfigurationId(anyLong())).thenReturn(false);
        assertThat(objectUnderTest.isSessionConfigurationInUse(1l)).isFalse();
        verify(sessionRepository, times(1)).existsBySessionConfigurationId(anyLong());
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void whenGetConfigForSession_verifySessionRepositoryCalledAndConfigReturns() {
        final var configuration = mock(SessionConfiguration.class);
        when(sessionRepository.findConfigurationBySessionId(PME_SESSION_ID))
                .thenReturn(Optional.of(configuration));

        assertThat(objectUnderTest.getConfigForSession(PME_SESSION_ID))
                .isEqualTo(configuration);

        verify(sessionRepository, times(1)).findConfigurationBySessionId(PME_SESSION_ID);
        verifyNoMoreInteractions(sessionRepository);
        verifyNoInteractions(configuration);
    }

    @Test
    void whenUpdateSessionToStopped_andSessionIsStarted_thenSessionIsUpdated() {
        when(sessionRepository.updateSessionToStoppedByIdAndClientId(PME_SESSION_ID, CLIENT_ID)).thenReturn(1);

        final var session = Session.builder()
                .id(PME_SESSION_ID)
                .clientId(CLIENT_APP_ID)
                .status(SessionStatus.STOPPED)
                .build();

        doReturn(Optional.of(session)).when(sessionRepository).findByClientIdAndId(CLIENT_ID, PME_SESSION_ID);

        assertThat(objectUnderTest.updateSessionToStopped(PME_SESSION_ID, CLIENT_ID)).isOne();
        assertThat(objectUnderTest.findByClientIdAndSessionId(CLIENT_ID, PME_SESSION_ID)).isEqualTo(Optional.of(session));
        verify(sessionRepository, times(1)).updateSessionToStoppedByIdAndClientId(PME_SESSION_ID, CLIENT_ID);
        verify(sessionRepository, times(1)).findByClientIdAndId(CLIENT_ID, PME_SESSION_ID);
        verifyNoMoreInteractions(sessionRepository);
        verify(mockMetricService, times(1)).increment(PME_SESSION_STOPPED_COUNT, 1);
    }

    @Test
    void whenUpdateSessionToStopped_andSessionIsStopped_thenSessionIsNotUpdated() {
        when(sessionRepository.updateSessionToStoppedByIdAndClientId(PME_SESSION_ID, CLIENT_ID)).thenReturn(0);

        assertThat(objectUnderTest.updateSessionToStopped(PME_SESSION_ID, CLIENT_ID)).isZero();
        verify(sessionRepository, times(1)).updateSessionToStoppedByIdAndClientId(PME_SESSION_ID, CLIENT_ID);
        verifyNoMoreInteractions(sessionRepository);
        verify(mockMetricService, times(1)).increment(PME_SESSION_STOPPED_COUNT, 0);
    }
}
