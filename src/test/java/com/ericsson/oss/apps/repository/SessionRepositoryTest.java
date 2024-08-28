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

package com.ericsson.oss.apps.repository;

import static com.ericsson.assertions.response.ResponseEntityAssertions.assertThat;
import static com.ericsson.oss.apps.model.SessionStatus.CREATED;
import static com.ericsson.oss.apps.model.SessionStatus.FINISHED;
import static com.ericsson.oss.apps.model.SessionStatus.STARTED;
import static com.ericsson.oss.apps.model.SessionStatus.STOPPED;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DURATION;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;

@ActiveProfiles("test")
@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SessionRepositoryTest {

    private static final LocalDateTime START_TIME = LocalDateTime.now();
    private static final String ALT_PME_SESSION_ID = "PME-5797a5db-client-app-execution-1";
    private static final String ALT_SESSION_REFERENCE = "client-app-execution-1";
    private static final List<SessionStatus> sessionStatuses = List.of(STOPPED, FINISHED);
    private static SessionConfiguration CONFIG;
    @Autowired
    private SessionConfigurationRepository sessionConfigurationRepository;

    @Autowired
    private SessionRepository objectUnderTest;

    @BeforeEach
    void setUp() {
        CONFIG = sessionConfigurationRepository
                .save(new SessionConfiguration("config", "SATURDAY,SUNDAY"));
        objectUnderTest.deleteAll();
    }

    @Test
    public void whenTwoSessionsExist_andOneFinishedAtTimeIsExpired_onlyTheExpiredSessionIsReturned() {
        final LocalDateTime createdAt = LocalDateTime.now().minusHours(21);
        final LocalDateTime expiredFinishedAt = LocalDateTime.now().minusHours(19);
        addSession(PME_SESSION_ID, SESSION_REFERENCE, createdAt, expiredFinishedAt, FINISHED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, createdAt, LocalDateTime.now(), FINISHED);

        final List<Session> sessions = objectUnderTest.findByStatusAndFinishedAtBefore(FINISHED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(1);
    }

    @Test
    public void whenTwoSessionsExist_andBothFinishedAtTimeIsExpired_bothSessionsAreReturned() {
        final LocalDateTime createdAt = LocalDateTime.now().minusHours(21);
        final LocalDateTime expiredFinishedAt = LocalDateTime.now().minusHours(19);
        addSession(PME_SESSION_ID, SESSION_REFERENCE, createdAt, expiredFinishedAt, FINISHED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, createdAt, expiredFinishedAt, FINISHED);

        final List<Session> sessions = objectUnderTest.findByStatusAndFinishedAtBefore(FINISHED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(2);
    }

    @Test
    public void whenTwoSessionsExist_andOneCreatedAtTimeIsExpired_onlyTheExpiredSessionIsReturned() {
        addSession(PME_SESSION_ID, SESSION_REFERENCE, LocalDateTime.now().minusHours(6), null, CREATED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, LocalDateTime.now().minusHours(4), null, CREATED);
        final List<Session> sessions = objectUnderTest.findByStatusAndCreatedAtBefore(CREATED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(1);
    }

    @Test
    public void whenTwoSessionsExist_andBothCreatedAtTimesAreExpired_bothSessionsAreReturned() {
        addSession(PME_SESSION_ID, SESSION_REFERENCE, LocalDateTime.now().minusHours(6), null, CREATED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, LocalDateTime.now().minusHours(4), null, CREATED);
        final List<Session> sessions = objectUnderTest.findByStatusAndCreatedAtBefore(CREATED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(1);
    }

    @Test
    public void whenTwoSessionsExist_andOneStateIsFinished_onlyTheFinishedSessionIsReturned() {
        final LocalDateTime createdAt = LocalDateTime.now().minusHours(21);
        final LocalDateTime expiredFinishedAt = LocalDateTime.now().minusHours(19);
        addSession(PME_SESSION_ID, SESSION_REFERENCE, createdAt, expiredFinishedAt, FINISHED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, createdAt, expiredFinishedAt, STOPPED);
        final List<Session> sessions = objectUnderTest.findByStatusAndFinishedAtBefore(FINISHED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(1);
    }

    @Test
    public void whenTwoSessionsExist_andOneStateIsStopped_onlyTheStoppedSessionIsReturned() {
        final LocalDateTime createdAt = LocalDateTime.now().minusHours(21);
        final LocalDateTime expiredFinishedAt = LocalDateTime.now().minusHours(19);
        addSession(PME_SESSION_ID, SESSION_REFERENCE, createdAt, expiredFinishedAt, FINISHED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, createdAt, expiredFinishedAt, STOPPED);
        final List<Session> sessions = objectUnderTest.findByStatusAndFinishedAtBefore(STOPPED, LocalDateTime.now().minusHours(5));
        assertThat(sessions.size()).isEqualTo(1);
    }

    @Test
    public void whenTwoSessionsExist_andBothAreStarted_bothSessionsAreReturned() {
        final LocalDateTime currentTime = LocalDateTime.now();
        addSession(PME_SESSION_ID, SESSION_REFERENCE, currentTime, currentTime.plusHours(DURATION), STARTED);
        addSession(ALT_PME_SESSION_ID, ALT_SESSION_REFERENCE, currentTime, currentTime.plusHours(DURATION), STARTED);
        final List<Session> sessions = objectUnderTest.findAllByStatus(STARTED);
        assertThat(sessions.size()).isEqualTo(2);
    }

    @Test
    public void whenFindByClientIdIsCalled_andASessionExistsForClientId_theSessionIsReturned() {
        final LocalDateTime currentTime = LocalDateTime.now();
        final Session session = addSession(PME_SESSION_ID, SESSION_REFERENCE, currentTime, currentTime.plusHours(DURATION), STARTED);
        final List<Session> sessions = objectUnderTest.findAllByClientId(CLIENT_APP_ID);
        assertThat(sessions).hasSize(1).containsExactly(session);
    }

    @Test
    public void whenFindByClientIdIsCalled_andNoSessionExistsForClientId_anEmptyListIsReturned() {
        final List<Session> sessions = objectUnderTest.findAllByClientId(CLIENT_APP_ID);
        assertThat(sessions).isEmpty();
    }

    @Test
    public void whenFindByClientIdAndSessionReferenceIsCalled_andASessionExistsForClientIdAndReference_theSessionIsReturned() {
        final LocalDateTime currentTime = LocalDateTime.now();
        final Session session = addSession(PME_SESSION_ID, SESSION_REFERENCE, currentTime, currentTime.plusHours(DURATION), STARTED);
        final List<Session> sessions = objectUnderTest.findAllByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE);
        assertThat(sessions).hasSize(1).containsExactly(session);
    }

    @Test
    public void whenFindByClientIdAndSessionReferenceIsCalled_andNoSessionExistsForClientIdAndReference_anEmptyListIsReturned() {
        final List<Session> sessions = objectUnderTest.findAllByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE);
        assertThat(sessions).isEmpty();
    }

    @Test
    public void whenFindByClientIdAndIdIsCalled_andASessionExistsForId_theSessionIsReturned() {
        final LocalDateTime currentTime = LocalDateTime.now();
        final Session session = addSession(PME_SESSION_ID, SESSION_REFERENCE, currentTime, currentTime.plusHours(DURATION), STARTED);
        final Optional<Session> sessionOptional = objectUnderTest.findByClientIdAndId(CLIENT_APP_ID, PME_SESSION_ID);
        assertThat(sessionOptional).isPresent().contains(session);
    }

    @Test
    public void whenFindBySessionIdIsCalled_andNoSessionExistsForId_anEmptyOptionalIsReturned() {
        final Optional<Session> sessionOptional = objectUnderTest.findByClientIdAndId(CLIENT_APP_ID, PME_SESSION_ID);
        assertThat(sessionOptional).isEmpty();
    }

    @Test
    void whenUpdateSessionToStartedById_andSessionExists_verifySessionIsUpdated() {
        final var session = objectUnderTest.save(defaultSession().build());

        assertThat(objectUnderTest.updateSessionToStartedById(session.getId()))
                .isOne();

        assertThat(objectUnderTest.getReferenceById(PME_SESSION_ID))
                .satisfies((persisted) -> {
                    assertThat(persisted.getStatus()).isEqualByComparingTo(STARTED);
                    assertThat(persisted.getStartedAt()).isNotNull();
                });
    }

    @Test
    void whenUpdateSessionToStartedById_andSessionNotExists_verifySessionIsNotUpdated() {
        objectUnderTest.save(defaultSession().build());

        assertThat(objectUnderTest.updateSessionToStartedById("unknownId"))
                .isZero();

        assertThat(objectUnderTest.getReferenceById(PME_SESSION_ID))
                .satisfies((persisted) -> {
                    assertThat(persisted.getStatus()).isEqualByComparingTo(CREATED);
                    assertThat(persisted.getStartedAt()).isNull();
                });
    }

    @Test
    void whenUpdateSessionToStartedById_andSessionIsNotCreated_verifySessionIsNotUpdated() {
        final var session = objectUnderTest.save(defaultSession().status(STARTED).build());
        assertThat(objectUnderTest.updateSessionToStartedById(session.getId())).isZero();
    }

    @Test
    void whenSessionExistsWithoutSessionConfiguration_thenReturnFalse() {
        objectUnderTest.save(defaultSession().status(STOPPED).build());
        assertThat(objectUnderTest.existsBySessionConfigurationId(100L)).isFalse();
    }

    @Test
    void whenSessionExistsWithSessionConfiguration_andSessionIsInStoppedState_thenReturnTrue() {
        objectUnderTest.save(defaultSession().status(STOPPED).build());
        assertThat(objectUnderTest.existsBySessionConfigurationId(CONFIG.getId())).isTrue();
    }

    @Test
    void whenSessionExistsWithSessionConfiguration_andSessionIsInFinishedState_thenReturnTrue() {
        objectUnderTest.save(defaultSession().status(FINISHED).build());
        assertThat(objectUnderTest.existsBySessionConfigurationId(CONFIG.getId())).isTrue();
    }

    @Test
    void whenSessionExistsWithSessionConfiguration_andSessionIsInStartedState_thenReturnTrue() {
        objectUnderTest.save(defaultSession().status(STARTED).build());
        assertThat(objectUnderTest.existsBySessionConfigurationId(CONFIG.getId())).isTrue();
    }

    @Test
    void whenSessionExistsWithSessionConfiguration_andSessionIsInCreatedState_thenReturnTrue() {
        objectUnderTest.save(defaultSession().status(CREATED).build());
        assertThat(objectUnderTest.existsBySessionConfigurationId(CONFIG.getId())).isTrue();
    }

    @Test
    void whenFindConfigurationBySessionId_andSessionExists_verifyConfigurationIsReturned() {
        objectUnderTest.save(defaultSession().build());
        assertThat(objectUnderTest.findConfigurationBySessionId(PME_SESSION_ID))
                .isPresent()
                .contains(CONFIG);
    }

    @Test
    void whenFindConfigurationBySessionId_andSessionNotExists_verifyEmptyIsReturned() {
        assertThat(objectUnderTest.findConfigurationBySessionId(PME_SESSION_ID))
                .isEmpty();
    }

    @Test
    void whenUpdateSessionToStoppedByIdAndClientId_andSessionExists_verifySessionIsUpdated() {
        final var session = objectUnderTest.save(defaultSession().build());

        assertThat(objectUnderTest.updateSessionToStoppedByIdAndClientId(session.getId(), session.getClientId()))
                .isOne();

        assertThat(objectUnderTest.getReferenceById(PME_SESSION_ID))
                .satisfies((persisted) -> {
                    assertThat(persisted.getStatus()).isEqualByComparingTo(STOPPED);
                    assertThat(persisted.getCreatedAt()).isNotNull();
                    assertThat(persisted.getFinishedAt()).isNotNull();
                });
    }

    @Test
    void whenUpdateSessionToStoppedByIdAndClientId_andSessionIsStopped_verifySessionIsNotUpdated() {
        final var session = objectUnderTest.save(defaultSession().status(STOPPED).build());
        assertThat(objectUnderTest.updateSessionToStoppedByIdAndClientId(session.getId(), session.getClientId())).isZero();
    }

    private Session addSession(final String id, final String sessionReference, final LocalDateTime createdAt, final LocalDateTime finishedAt,
            final SessionStatus status) {
        final Session session = Session.builder()
                .id(id)
                .clientId(CLIENT_APP_ID)
                .sessionReference(sessionReference)
                .duration(DURATION)
                .sessionConfiguration(CONFIG)
                .createdAt(createdAt)
                .startedAt(createdAt)
                .finishedAt(finishedAt)
                .status(status)
                .build();
        objectUnderTest.save(session);
        return session;
    }

    private Session.SessionBuilder defaultSession() {
        return Session.builder()
                .id(PME_SESSION_ID)
                .clientId(CLIENT_APP_ID)
                .sessionReference(SESSION_REFERENCE)
                .sessionConfiguration(CONFIG)
                .duration(18)
                .status(CREATED)
                .createdAt(START_TIME);
    }

}
