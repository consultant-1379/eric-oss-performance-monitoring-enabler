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

import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLCU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_TDD;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.kafka.InsecureVerdictKafkaProducer;
import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.VerdictMessage;

@ExtendWith(MockitoExtension.class)
public class VerdictCalculationServiceTest {
    private static final ZonedDateTime EXECUTION_TIME = ZonedDateTime.of(2023, 12, 5, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final ZonedDateTime EXECUTION_END_TIME = EXECUTION_TIME.plusHours(1).minusSeconds(1);

    @Mock
    private MonitoringObjectService monitoringObjectService;
    @Mock
    private SessionService sessionService;
    @Mock
    private InsecureVerdictKafkaProducer kafkaProducer;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private VerdictCalculationService objectUnderTest;

    final ArgumentCaptor<VerdictMessage> verdictCaptor = ArgumentCaptor.forClass(VerdictMessage.class);

    @Test
    void whenCalculateVerdictsIsCalled_thenTheLastProcessedTimeOfTheMonitoringObjectIsUpdated() {
        mockConfig();
        final var monitoringObjects = List.of(
                mockMonitoringObject(FDN_FDD),
                mockMonitoringObject(FDN_TDD),
                mockMonitoringObject(FDN_NRCELLCU)
        );

        final var inOrder = inOrder(sessionService, kafkaProducer, monitoringObjectService);

        objectUnderTest.calculateVerdicts(PME_SESSION_ID, monitoringObjects, new HashMap<>(), EXECUTION_TIME, EXECUTION_END_TIME);

        inOrder.verify(sessionService, times(1)).getConfigForSession(PME_SESSION_ID);
        inOrder.verify(kafkaProducer, times(3)).sendVerdict(verdictCaptor.capture());
        inOrder.verify(monitoringObjectService, times(1)).updateLastProcessedTimes(any(), eq(EXECUTION_END_TIME));

        assertThat(verdictCaptor.getAllValues())
                .extracting(v -> v.getFdn().toString())
                .containsExactlyInAnyOrder(FDN_FDD, FDN_TDD, FDN_NRCELLCU);
    }

    private MonitoringObject mockMonitoringObject(final String fdn) {
        final var monitoringObject = mock(MonitoringObject.class);
        when(monitoringObject.getFdn()).thenReturn(fdn);
        return monitoringObject;
    }

    private void mockConfig() {
        final var config = mock(SessionConfiguration.class);
        when(config.getKpiConfigs()).thenReturn(List.of());
        when(sessionService.getConfigForSession(PME_SESSION_ID)).thenReturn(config);
    }
}
