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

package com.ericsson.oss.apps.repository;

import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLCU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_TDD;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.StateEnum;

import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
@DataJpaTest
@ActiveProfiles("test")
@Import(RepositoryTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MonitoringObjectRepositoryTest {

    private static final ZonedDateTime START_TIME = ZonedDateTime.now();
    private static final String ALT_PME_SESSION_ID = "PME-5797a5db-client-app-execution-1";
    @Autowired
    private MonitoringObjectRepository objectUnderTest;

    @BeforeEach
    public void clearMonitoringObjects() {
        objectUnderTest.deleteAll();
    }

    @Test
    public void whenTwoMonitoringObjectsArePresent_andThePmeSessionIdsAreDifferent_thenOnlyOneIsDeleted() {
        addMonitoringObject(FDN_FDD, PME_SESSION_ID);
        addMonitoringObject(FDN_TDD, ALT_PME_SESSION_ID);
        assertThat(objectUnderTest.count()).isEqualTo(2);
        final long removed = objectUnderTest.deleteByPmeSessionId(PME_SESSION_ID);
        assertThat(removed).isEqualTo(1L);
        assertThat(objectUnderTest.count()).isEqualTo(1);
    }

    @Test
    public void whenMultipleMonitoringObjectsExist_andHaveTheSamePeSessionId_thenAllOfThemAreRemoved() {
        addMonitoringObject(FDN_FDD, PME_SESSION_ID);
        addMonitoringObject(FDN_TDD, PME_SESSION_ID);
        addMonitoringObject(FDN_NRCELLCU, PME_SESSION_ID);
        assertThat(objectUnderTest.count()).isEqualTo(3);
        final long removed = objectUnderTest.deleteByPmeSessionId(PME_SESSION_ID);
        assertThat(removed).isEqualTo(3L);
        assertThat(objectUnderTest.count()).isEqualTo(0);
    }

    @Test
    void whenFindAllBySessionId_andMonitoringObjectsExistForId_thenMonitoringObjectsReturned() {
        final var monitoringObjects = List.of(
                new MonitoringObject(FDN_FDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null),
                new MonitoringObject(FDN_TDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null),
                new MonitoringObject(FDN_NRCELLCU, PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null));

        objectUnderTest.saveAll(monitoringObjects);
        objectUnderTest.saveAll(List.of(
                new MonitoringObject(FDN_FDD, ALT_PME_SESSION_ID, StateEnum.ENABLED, START_TIME.minusHours(2), null, null),
                new MonitoringObject(FDN_TDD, ALT_PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null)));

        assertThat(objectUnderTest.count())
                .isEqualTo(5);

        assertThat(objectUnderTest.findAllByPmeSessionId(PME_SESSION_ID))
                .hasSize(3)
                .containsExactlyInAnyOrderElementsOf(monitoringObjects);
    }

    @Test
    void whenFindAllBySessionId_andMonitoringObjectsDoNotExistForId_thenNoMonitoringObjectsReturned() {
        objectUnderTest.saveAll(List.of(
                new MonitoringObject(FDN_FDD, ALT_PME_SESSION_ID, StateEnum.ENABLED, START_TIME.minusHours(2), null, null),
                new MonitoringObject(FDN_TDD, ALT_PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null)));

        assertThat(objectUnderTest.count())
                .isEqualTo(2);

        assertThat(objectUnderTest.findAllByPmeSessionId(PME_SESSION_ID))
                .isEmpty();
    }

    @Test
    void whenFindAllByStateAndPmeSessionIdIn_forSingleSessionId_thenOnlyEnabledForOneSessionReturned() {
        final var monitoringObjects = List.of(
                new MonitoringObject(FDN_FDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null),
                new MonitoringObject(FDN_TDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null));

        objectUnderTest.saveAll(monitoringObjects);
        objectUnderTest.saveAll(List.of(
                new MonitoringObject(FDN_FDD, ALT_PME_SESSION_ID, StateEnum.ENABLED, START_TIME.minusHours(2), null, null),
                new MonitoringObject(FDN_TDD, ALT_PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null),
                new MonitoringObject(FDN_NRCELLCU, PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null)));

        assertThat(objectUnderTest.count())
                .isEqualTo(5);

        assertThat(objectUnderTest.findAllByStateAndPmeSessionIdIn(StateEnum.ENABLED, List.of(PME_SESSION_ID)))
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(monitoringObjects);
    }

    @Test
    void whenFindAllByStateAndPmeSessionIdIn_forMultipleSessionId_thenOnlyEnabledForBothSessionsReturned() {
        final var monitoringObjects = List.of(
                new MonitoringObject(FDN_FDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null),
                new MonitoringObject(FDN_TDD, PME_SESSION_ID, StateEnum.ENABLED, START_TIME, null, null),
                new MonitoringObject(FDN_FDD, ALT_PME_SESSION_ID, StateEnum.ENABLED, START_TIME.minusHours(2), null, null));

        objectUnderTest.saveAll(monitoringObjects);
        objectUnderTest.saveAll(List.of(
                new MonitoringObject(FDN_TDD, ALT_PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null),
                new MonitoringObject(FDN_NRCELLCU, PME_SESSION_ID, StateEnum.STOPPED, START_TIME, null, null)));

        assertThat(objectUnderTest.count())
                .isEqualTo(5);

        assertThat(objectUnderTest.findAllByStateAndPmeSessionIdIn(StateEnum.ENABLED, List.of(PME_SESSION_ID, ALT_PME_SESSION_ID)))
                .hasSize(3)
                .containsExactlyInAnyOrderElementsOf(monitoringObjects);
    }

    public void addMonitoringObject(final String fdn, final String pmeSessionId) {
        final MonitoringObject mo = MonitoringObject.builder()
                .fdn(fdn)
                .pmeSessionId(pmeSessionId)
                .state(StateEnum.ENABLED)
                .lastProcessedTime(ZonedDateTime.now())
                .startTime(ZonedDateTime.now())
                .build();
        objectUnderTest.save(mo);
    }

}
