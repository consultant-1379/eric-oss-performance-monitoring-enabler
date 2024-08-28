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
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID_2;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID_3;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;

@ExtendWith(MockitoExtension.class)
public class MonitoringObjectServiceTest {

    @Mock
    private MonitoringObjectRepository moRepository;

    private MonitoringObjectService objectUnderTest;

    @BeforeEach
    void setUp() {
        objectUnderTest = new MonitoringObjectService(moRepository);
    }

    @Test
    void whenUpdateLastProcessedTimeIsCalled_andNoPreviousLastProcessedTimeExists_theLastProcessedTimeIsAdded() {
        final ZonedDateTime startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(1);
        final MonitoringObject mo = new MonitoringObject();
        final ZonedDateTime lastProcessedTime = startTime.plusHours(1L).minusSeconds(1L);
        mo.setFdn(FDN_FDD);
        mo.setPmeSessionId(PME_SESSION_ID);
        mo.setState(StateEnum.ENABLED);
        mo.setStartTime(startTime);
        objectUnderTest.updateLastProcessedTimes(Collections.singletonList(mo), lastProcessedTime);
        assertThat(mo.getLastProcessedTime())
                .isEqualTo(lastProcessedTime);
    }

    @Test
    void whenUpdateLastProcessedTimeIsCalled_andLastProcessedTimeExists_theLastProcessedTimeIsIncremented() {
        final ZonedDateTime startTime = ZonedDateTime.now().truncatedTo(HOURS).minusHours(1);
        final MonitoringObject mo = new MonitoringObject();
        final ZonedDateTime lastProcessedTime = startTime.plusHours(2L).minusSeconds(1L);
        mo.setFdn(FDN_FDD);
        mo.setPmeSessionId(PME_SESSION_ID);
        mo.setState(StateEnum.ENABLED);
        mo.setStartTime(startTime);
        mo.setLastProcessedTime(startTime.plusMinutes(59L));
        objectUnderTest.updateLastProcessedTimes(Collections.singletonList(mo), lastProcessedTime);
        assertThat(mo.getLastProcessedTime())
                .isEqualTo(lastProcessedTime);
    }

    @Test
    void whenUpdateMonitoringObjectStates_thenMonitoringObjectStatesUpdatedCorrectly() {
        final List<String> sessionIds = Arrays.asList(PME_SESSION_ID, PME_SESSION_ID_2, PME_SESSION_ID_3);

        objectUnderTest.updateMonitoringObjectStates(sessionIds);

        verify(moRepository, times(1)).updateMonitoringObjectStateByPmeSessionId(PME_SESSION_ID);
        verify(moRepository, times(1)).updateMonitoringObjectStateByPmeSessionId(PME_SESSION_ID_2);
        verify(moRepository, times(1)).updateMonitoringObjectStateByPmeSessionId(PME_SESSION_ID_3);
    }
}
