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

import static com.ericsson.assertions.EpmeAssertions.assertThat;
import static com.ericsson.oss.apps.model.SessionStatus.STARTED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_NULL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_RETRIEVAL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_LOOKBACK_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_PA_EXECUTION_TIME_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DEFAULT_DURATION_L;
import static com.ericsson.oss.apps.util.TestConstants.DURATION;
import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLCU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_TDD;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.exception.KpiCalculationMonitoringHandlingException;
import com.ericsson.oss.apps.exception.KpiQueryHandlingException;
import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.service.KpiCalculationsMonitoringService;
import com.ericsson.oss.apps.service.KpiQueryService;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.MonitoringObjectService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.StartupService;
import com.ericsson.oss.apps.service.VerdictCalculationService;

@DirtiesContext
@SpringBootTest(properties = {
        "execution.schedule.cron-expression=*/1 * * * * *",
        "queryKpis.batchSize=2"
})
@ActiveProfiles("test")
class SchedulingJobTest {
    private final ZonedDateTime now = ZonedDateTime.now().truncatedTo(HOURS);
    private final LocalDateTime currentTime = LocalDateTime.now();

    @Value("${queryKpis.batchSize}")
    private long batchSize;
    @MockBean
    private SessionService sessionService;
    @MockBean
    private MetricService metricService;
    @MockBean
    private MonitoringObjectService monitoringObjectService;
    @MockBean
    private MonitoringObjectRepository monitoringObjectRepository;
    @MockBean
    private KpiQueryService kpiQueryService;
    @MockBean
    private VerdictCalculationService verdictCalculationService;
    @MockBean
    private KpiCalculationsMonitoringService KpiCalculationsMonitoringService;
    @MockBean
    private StartupService startupService;

    @SpyBean
    private SchedulingJob objectUnderTest;

    @Test
    void whenExecuteScheduledMethod_andAwaitTenSeconds_verifyInvocations() {
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(objectUnderTest, atLeast(9)).executeScheduledMethod());
    }

    @Test
    void whenExecuteScheduledMethod_verifySessionServiceMoRepositoryInvocations() throws InterruptedException {
        final List<Session> session = Collections.singletonList(createMockSession(
                currentTime, "startedSession", "startedSession"));
        when(sessionService.findAllStarted()).thenReturn(session);
        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList())).thenReturn(
                List.of(mockMonitoringObject("objectAfterEndtime", "startedSession", now.minusHours(5), null, 1L)));

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        verify(sessionService, atLeast(1)).findAllStarted();
        verify(sessionService, atLeast(1)).updateSessionsWithNoEnabledMonitoringObjects(anyList());
        verify(monitoringObjectService, atLeast(1)).updateMonitoringObjectStates(anyList());
        verify(monitoringObjectRepository, atLeast(1)).findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList());
        verifyNoMoreInteractions(sessionService);
        verifyNoMoreInteractions(monitoringObjectService);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_COUNT), any());
        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenLastProcessedTimeisNull_andOutsideMonitoringWindow_thenFilterOut() {
        final var filterMonitoringObjectsForCurrentHour = objectUnderTest.filterMonitoringObjectsForHour(now, List.of(
                mockMonitoringObject("objectOutsideMonitoringWindow", "startedSession", now.plusHours(5), null, DEFAULT_DURATION_L)));
        assertThat(filterMonitoringObjectsForCurrentHour).isEmpty();
    }

    @Test
    void whenLastProcessedTimeisNull_andInsideMonitoringWindow_thenProcess() {
        final var filterMonitoringObjectsForCurrentHour = objectUnderTest.filterMonitoringObjectsForHour(now, List.of(
                mockMonitoringObject("mo7", "startedSession", now.minusHours(5), null, DEFAULT_DURATION_L)));
        assertThat(filterMonitoringObjectsForCurrentHour.size()).isEqualTo(1);
    }

    @Test
    void whenExecutionLaterThanEndTime_thenFilterOut() {
        final var filterMonitoringObjectsForCurrentHour = objectUnderTest.filterMonitoringObjectsForHour(now, List.of(
                mockMonitoringObject("mo7", "startedSession", now.minusHours(21), null, DEFAULT_DURATION_L)));
        assertThat(filterMonitoringObjectsForCurrentHour).isEmpty();
    }

    @Test
    void whenExecutionTimesSameAsStartTime_AndExecutionTimeEarlierThanEndTime_thenProcess() {
        final var filterMonitoringObjectsForCurrentHour = objectUnderTest.filterMonitoringObjectsForHour(now, List.of(
                mockMonitoringObject("mo7", "startedSession", now, null, DEFAULT_DURATION_L)));
        assertThat(filterMonitoringObjectsForCurrentHour.size()).isEqualTo(1);
    }

    @Test
    void whenTwoSessionsStarted_andSevenMos_thenFilterAndGroupBySession() throws InterruptedException {
        final var monitoringObjects = List.of(
                mockMonitoringObject("mo1", "startedSession1", now.minusHours(1), null, DEFAULT_DURATION_L),
                mockMonitoringObject("mo2", "startedSession1", now.minusHours(5), 4L, DEFAULT_DURATION_L),
                mockMonitoringObject("mo3", "startedSession1", now.minusHours(2), 1L, DEFAULT_DURATION_L),
                mockMonitoringObject("mo4", "startedSession1", now.minusHours(5), 6L, DEFAULT_DURATION_L),
                mockMonitoringObject("mo5", "startedSession2", now.plusHours(1), null, DEFAULT_DURATION_L),
                mockMonitoringObject("mo6", "startedSession2", now.minusHours(5), 2L, DEFAULT_DURATION_L),
                mockMonitoringObject("mo7", "startedSession2", now.minusHours(5), 3L, DEFAULT_DURATION_L),
                mockMonitoringObject("mo8", "startedSession1", now.minusHours(5), 5L, DEFAULT_DURATION_L));

        final var filterMonitoringObjectsForCurrentHour = objectUnderTest.filterMonitoringObjectsForHour(now, monitoringObjects);
        final var filterMonitoringObjectsForLastHour = objectUnderTest.filterMonitoringObjectsForHour(now.minusHours(1), monitoringObjects);
        final var filterMonitoringObjectsForTwoHourAgo = objectUnderTest.filterMonitoringObjectsForHour(now.minusHours(2), monitoringObjects);

        objectUnderTest.executeForHour(filterMonitoringObjectsForTwoHourAgo, now.minusHours(2), false, 240);
        objectUnderTest.executeForHour(filterMonitoringObjectsForLastHour, now.minusHours(1), false, 180);
        objectUnderTest.executeForHour(filterMonitoringObjectsForCurrentHour, now, false, 120);

        assertThat(filterMonitoringObjectsForCurrentHour)
                .hasSize(2)
                .hasEntrySatisfying("startedSession1", mosForSession -> assertThat(mosForSession).hasSize(4))
                .hasEntrySatisfying("startedSession2", mosForSession -> assertThat(mosForSession).hasSize(2));

        assertThat(filterMonitoringObjectsForLastHour)
                .hasSize(2)
                .hasEntrySatisfying("startedSession1", mosForSession -> assertThat(mosForSession).hasSize(3))
                .hasEntrySatisfying("startedSession2", mosForSession -> assertThat(mosForSession).hasSize(2));

        assertThat(filterMonitoringObjectsForTwoHourAgo)
                .hasSize(1)
                .doesNotContainKey("startedSession1")
                .hasEntrySatisfying("startedSession2", mosForSession -> assertThat(mosForSession).hasSize(2));

        verify(objectUnderTest, times(3)).executeForHour(any(), any(), any(), anyInt());

        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(2.0));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(5.0));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(6.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
    }

    @Test
    void whenExecuteScheduleMethod_verifyQueryServiceFetchAndFilterIsCalled() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(3);

        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(
                        mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, 2L, 8L),
                        mockMonitoringObject(FDN_TDD, PME_SESSION_ID, startTime, 2L, 8L),
                        mockMonitoringObject(FDN_NRCELLCU, PME_SESSION_ID, startTime, 2L, 8L)));

        when(kpiQueryService.queryKpis(any(ZonedDateTime.class), anyLong(), anyLong()))
                .thenReturn(Map.of(FDN_FDD, new KpiResult(), FDN_TDD, new KpiResult()))
                .thenReturn(Map.of(FDN_NRCELLCU, new KpiResult()));

        when(kpiQueryService.filterKpis(anyMap(), anySet())).thenCallRealMethod();

        final var inOrder = inOrder(kpiQueryService);

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now().truncatedTo(HOURS).minusHours(3)));

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        inOrder.verify(kpiQueryService, times(1)).filterKpis(anyMap(), anySet());
        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(batchSize));
        inOrder.verify(kpiQueryService, times(1)).filterKpis(anyMap(), anySet());
        inOrder.verifyNoMoreInteractions();

        verify(metricService, times(1)).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), eq(1.0));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), eq(2.0));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(0.0));
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 3);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());
        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_withInvalidFdns_verifyQueryServiceIsCalledTwice() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(3);

        final var monitoringObject = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, 2L, 8L);
        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObject));

        when(kpiQueryService.queryKpis(any(ZonedDateTime.class), anyLong(), anyLong()))
                .thenReturn(Map.of(FDN_FDD, new KpiResult()))
                .thenReturn(Collections.emptyMap());

        final var inOrder = inOrder(kpiQueryService);

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now().truncatedTo(HOURS).minusHours(3)));

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        inOrder.verify(kpiQueryService, times(1)).filterKpis(anyMap(), anySet());
        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(batchSize));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(1.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
        inOrder.verifyNoMoreInteractions();
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());
        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andQueryServiceReturnsNoKpis_verifyQueryServiceFilterIsNotCalled() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(3);

        final var monitoringObject = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, 2L, 8L);
        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObject));

        when(kpiQueryService.queryKpis(any(ZonedDateTime.class), anyLong(), anyLong()))
                .thenReturn(Collections.emptyMap());

        final var inOrder = inOrder(kpiQueryService);

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now().truncatedTo(HOURS).minusHours(3)));

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        inOrder.verify(kpiQueryService, never()).filterKpis(anyMap(), anySet());
        inOrder.verifyNoMoreInteractions();
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(1.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());
        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andQueryServiceThrowsException_verifyQueryServiceFilterIsNotCalled() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(3);

        final var monitoringObject = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, 2L, 8L);
        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObject));

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenReturn(List.of(ZonedDateTime.now().truncatedTo(HOURS).minusHours(3)));

        when(kpiQueryService.queryKpis(any(ZonedDateTime.class), anyLong(), anyLong()))
                .thenThrow(KpiQueryHandlingException.class);

        final var inOrder = inOrder(kpiQueryService);

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(kpiQueryService, times(1)).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        inOrder.verify(kpiQueryService, never()).filterKpis(anyMap(), anySet());
        inOrder.verifyNoMoreInteractions();
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());

        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andNoKpisAreReady_thenQueryKpisIsNotCalled() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(3);

        final var monitoringObject = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, 2L, 8L);
        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObject));

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenReturn(Collections.emptyList());

        final var inOrder = inOrder(kpiQueryService);

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(kpiQueryService, never()).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        inOrder.verify(kpiQueryService, never()).filterKpis(anyMap(), anySet());
        inOrder.verifyNoMoreInteractions();
        verify(verdictCalculationService, atLeast(1)).calculateVerdicts(any(String.class), any(List.class), any(Map.class), any(ZonedDateTime.class),
                any(ZonedDateTime.class));
        verify(metricService, times(1)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(1.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 1);
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), any());

        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_thenCheckKpiReadinessIsCalledCorrectlyForCurrentAndLookBackHours() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTimeCurrent = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(1);
        final var startTimeLookBackOne = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(2);
        final var startTimeLookBackTwo = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(3);

        final var monitoringObjectCurrent = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeCurrent, null, 8L);
        final var monitoringObjectLookBackOne = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeLookBackOne, null, 8L);
        final var monitoringObjectLookBackTwo = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeLookBackTwo, null, 8L);

        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObjectCurrent, monitoringObjectLookBackOne, monitoringObjectLookBackTwo));

        final var inOrder = inOrder(KpiCalculationsMonitoringService);

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        inOrder.verify(KpiCalculationsMonitoringService, times(1)).checkKpiReadiness(startTimeLookBackTwo, false, 240);
        inOrder.verify(KpiCalculationsMonitoringService, times(1)).checkKpiReadiness(startTimeLookBackOne, false, 180);
        inOrder.verify(KpiCalculationsMonitoringService, times(1)).checkKpiReadiness(startTimeCurrent, true, 120);
        verify(metricService, times(3)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(1.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
        verify(metricService, times(1)).increment(PME_MO_MONITORED_LOOKBACK_COUNT, 1);
        verify(metricService, times(1)).increment(PME_MO_MONITORED_LOOKBACK_COUNT, 2);
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 3);

        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andKpiCalculationsMonitoringServiceThrowsException_thenReturnKpisNotReady() throws InterruptedException {

        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTimeCurrent = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(1);
        final var startTimeLookBackOne = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(2);
        final var startTimeLookBackTwo = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(3);

        final var monitoringObjectCurrent = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeCurrent, null, 8L);
        final var monitoringObjectLookBackOne = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeLookBackOne, null, 8L);
        final var monitoringObjectLookBackTwo = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTimeLookBackTwo, null, 8L);

        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObjectCurrent, monitoringObjectLookBackOne, monitoringObjectLookBackTwo));

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenThrow(KpiCalculationMonitoringHandlingException.class);

        doNothing().when(startupService).verifyServiceReady();

        objectUnderTest.executeScheduledMethod();

        verify(kpiQueryService, never()).queryKpis(any(ZonedDateTime.class), eq(batchSize), eq(0L));
        verify(verdictCalculationService, atLeast(1)).calculateVerdicts(any(String.class), any(List.class), any(Map.class), any(ZonedDateTime.class),
                any(ZonedDateTime.class));
        verify(metricService, times(3)).increment(eq(PME_MO_KPI_NULL_COUNT), eq(1.0));
        verify(metricService, never()).increment(eq(PME_MO_KPI_RETRIEVAL_COUNT), any());
        verify(metricService, times(1)).increment(PME_MO_MONITORED_LOOKBACK_COUNT, 1);
        verify(metricService, times(1)).increment(PME_MO_MONITORED_LOOKBACK_COUNT, 2);
        verify(metricService, times(1)).increment(PME_MO_MONITORED_COUNT, 3);

        verifyTimerMetrics(0, 1, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andServiceisNotReady_thenSkippedTimerMetricIsInvoked() throws InterruptedException {
        doThrow(ControllerDetailException.class).when(startupService).verifyServiceReady();

        try {
            objectUnderTest.executeScheduledMethod();
        } catch (final ControllerDetailException e) {
        }

        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), anyDouble());
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_COUNT), anyDouble());

        verifyTimerMetrics(0, 0, 1);
    }

    @Test
    void whenExecuteScheduleMethod_andExecutionFails_thenFailedTimerMetricIsInvoked() throws InterruptedException {
        final var session = mock(Session.class);
        when(session.getId()).thenReturn(PME_SESSION_ID);
        when(sessionService.findAllStarted())
                .thenReturn(List.of(session));

        final var startTime = Instant.now().truncatedTo(HOURS).atZone(ZoneOffset.UTC).minusHours(1);

        final var monitoringObject = mockMonitoringObject(FDN_FDD, PME_SESSION_ID, startTime, null, 8L);

        when(monitoringObjectRepository.findAllByStateAndPmeSessionIdIn(eq(StateEnum.ENABLED), anyList()))
                .thenReturn(List.of(monitoringObject));

        when(KpiCalculationsMonitoringService.checkKpiReadiness(any(ZonedDateTime.class), anyBoolean(), anyInt()))
                .thenThrow(KpiCalculationMonitoringHandlingException.class);

        doThrow(new InterruptedException()).when(KpiCalculationsMonitoringService).checkKpiReadiness(any(), anyBoolean(), anyInt());

        try {
            objectUnderTest.executeScheduledMethod();
        } catch (final InterruptedException e) {
        }

        verifyTimerMetrics(1, 0, 0);
    }

    @Test
    void whenExecuteScheduleMethod_andThereAreMonitoringObjectsForCurrentExecutionTime_thenSkippedTimerMetricIsInvoked() throws InterruptedException {
        objectUnderTest.executeScheduledMethod();

        verify(metricService, never()).increment(eq(PME_MO_MONITORED_LOOKBACK_COUNT), anyDouble());
        verify(metricService, never()).increment(eq(PME_MO_MONITORED_COUNT), anyDouble());

        verifyTimerMetrics(0, 0, 1);
    }

    private Session createMockSession(final LocalDateTime createdAt, final String id, final String sessionReference) {
        final SessionConfiguration config = new SessionConfiguration();
        config.setId(1L);
        return Session.builder()
                .id(id)
                .clientId(CLIENT_APP_ID)
                .sessionReference(sessionReference)
                .duration(DURATION)
                .sessionConfiguration(config)
                .createdAt(createdAt)
                .startedAt(null)
                .finishedAt(null)
                .status(STARTED)
                .build();
    }

    private MonitoringObject mockMonitoringObject(final String fdn, final String sessionId, final ZonedDateTime start,
            final Long lastProcessedOffset, final Long endOffset) {
        return MonitoringObject.builder()
                .fdn(fdn)
                .pmeSessionId(sessionId)
                .state(StateEnum.ENABLED)
                .startTime(start)
                .lastProcessedTime(Objects.isNull(lastProcessedOffset) ? null : start.plusHours(lastProcessedOffset).minusSeconds(1))
                .endTime(start.plusHours(endOffset).minusSeconds(1))
                .build();
    }

    private void verifyTimerMetrics(final int failed, final int succeeded, final int skipped) {
        verify(metricService, times(1)).startTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, FAILED);
        verify(metricService, times(1)).startTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
        verify(metricService, times(1)).startTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SKIPPED);
        verify(metricService, times(failed))
                .stopTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, FAILED);
        verify(metricService, times(skipped))
                .stopTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SKIPPED);
        verify(metricService, times(succeeded))
                .stopTimer(String.valueOf(objectUnderTest.hashCode()), PME_PA_EXECUTION_TIME_HOURLY, STATUS, SUCCEEDED);
    }
}
