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
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_COUNT;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;

@ExtendWith(MockitoExtension.class)
public class MonitoringObjectKafkaServiceTest {

    private static final String FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00001";

    @Mock
    private MonitoringObjectRepository monitoringObjectRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private MonitoringObjectKafkaService objectUnderTest;

    @Test
    void whenMonitoringObjectIsConsumed_andPmeSessionIdWasNotFound_thenNothingIsPersisted() {
        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        when(sessionService.findById(PME_SESSION_ID)).thenReturn(Optional.empty());

        objectUnderTest.consume(moMessage);

        verify(sessionService, times(1)).findById(PME_SESSION_ID);
        verify(monitoringObjectRepository, never()).save(ArgumentMatchers.any(MonitoringObject.class));
        verify(metricService, times(1)).increment(PME_MO_DISCARDED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_COUNT), anyDouble());
    }

    @Test
    void whenMonitoringObjectIsConsumed_andSessionHasStopped_thenNothingIsPersisted() {
        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        final Session session = mock(Session.class);
        when(session.getStatus()).thenReturn(SessionStatus.STOPPED);
        when(sessionService.findById(PME_SESSION_ID)).thenReturn(Optional.of(session));

        objectUnderTest.consume(moMessage);

        verify(sessionService, times(1)).findById(PME_SESSION_ID);
        verify(monitoringObjectRepository, never()).save(ArgumentMatchers.any(MonitoringObject.class));
        verify(metricService, times(1)).increment(PME_MO_DISCARDED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_COUNT), anyDouble());
    }

    @Test
    void whenMonitoringObjectIsConsumed_andSessionHasFinished_thenNothingIsPersisted() {
        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        final Session session = mock(Session.class);
        when(session.getStatus()).thenReturn(SessionStatus.FINISHED);
        when(sessionService.findById(PME_SESSION_ID)).thenReturn(Optional.of(session));

        objectUnderTest.consume(moMessage);

        verify(sessionService, times(1)).findById(PME_SESSION_ID);
        verify(monitoringObjectRepository, never()).save(ArgumentMatchers.any(MonitoringObject.class));
        verify(metricService, times(1)).increment(PME_MO_DISCARDED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_COUNT), anyDouble());
    }

    @Test
    void whenMonitoringObjectIsConsumed_andStateIsEnabledAndSessionHasFinished_thenMonitoringObjectIsPersisted() {
        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        final Session session = mock(Session.class);
        when(session.getStatus()).thenReturn(SessionStatus.CREATED);
        when(sessionService.findById(PME_SESSION_ID)).thenReturn(Optional.of(session));

        objectUnderTest.consume(moMessage);

        verify(sessionService, times(1)).findById(PME_SESSION_ID);
        verify(monitoringObjectRepository, times(1)).save(ArgumentMatchers.any(MonitoringObject.class));
        verify(sessionService, times(1)).updateSessionToStarted(PME_SESSION_ID);
        verify(metricService, times(1)).increment(PME_MO_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_DISCARDED_COUNT), anyDouble());
    }

    @Test
    void whenMonitoringObjectIsConsumed_andStateIsStoppedAndSessionHasFinished_thenMonitoringObjectIsPersisted() {
        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.STOPPED)
                .build();

        final Session session = mock(Session.class);
        when(session.getStatus()).thenReturn(SessionStatus.STARTED);
        when(sessionService.findById(PME_SESSION_ID)).thenReturn(Optional.of(session));

        objectUnderTest.consume(moMessage);

        verify(sessionService, times(1)).findById(PME_SESSION_ID);
        verify(monitoringObjectRepository, times(1)).save(ArgumentMatchers.any(MonitoringObject.class));
        verify(metricService, times(1)).increment(PME_MO_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_DISCARDED_COUNT), anyDouble());
    }
}