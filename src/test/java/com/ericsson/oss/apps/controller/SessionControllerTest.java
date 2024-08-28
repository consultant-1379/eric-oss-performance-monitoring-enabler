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

package com.ericsson.oss.apps.controller;

import static com.ericsson.assertions.EpmeAssertions.assertThat;
import static com.ericsson.oss.apps.model.SessionStatus.CREATED;
import static com.ericsson.oss.apps.model.SessionStatus.FINISHED;
import static com.ericsson.oss.apps.model.SessionStatus.STOPPED;
import static com.ericsson.oss.apps.util.Constants.CLIENT_ID_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.PME_CONFIG_ID_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.SESSION_ALREADY_EXISTS;
import static com.ericsson.oss.apps.util.Constants.SESSION_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.SESSION_IS_STOPPED;
import static com.ericsson.oss.apps.util.Constants.SESSION_UPDATE_FAILED;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.TestConstants.APPLICATION_JSON;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DETAIL_FIELD;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.REASON_FIELD;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static com.ericsson.oss.apps.util.TestConstants.STATUS_FIELD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.api.model.EpmeSession;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.api.model.EpmeSessionStopRequest;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.service.ConfigurationService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.ValidationService;

/**
 * Unit tests for {@link SessionController} class
 */
@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    final private LocalDateTime timestamp = LocalDateTime.now();
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private SessionService sessionService;
    @Mock
    private ValidationService validationService;

    @InjectMocks
    private SessionController objectUnderTest;

    @Test
    void whenPostSessionsEndpoint_andSessionIsValid_thenCreatedIsReturned() {
        when(sessionService.exists(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(false);
        final var config = mock(SessionConfiguration.class);
        when(configurationService.getConfig(String.valueOf(PME_CONFIGURATION_ID)))
                .thenReturn(config);
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));
        final var session = mock(Session.class);
        final var sessionDto = mock(EpmeSession.class);
        when(sessionService.createSession(CLIENT_APP_ID, sessionRequest, config)).thenReturn(session);
        when(sessionService.dto(session)).thenReturn(sessionDto);

        final var inOrder = inOrder(configurationService, sessionService);

        assertThat(objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(HttpStatus.CREATED)
                .hasBody(sessionDto);

        inOrder.verify(sessionService, times(1)).exists(CLIENT_APP_ID, SESSION_REFERENCE);
        inOrder.verify(configurationService, times(1)).getConfig(String.valueOf(PME_CONFIGURATION_ID));
        inOrder.verify(sessionService, times(1)).createSession(CLIENT_APP_ID, sessionRequest, config);
        inOrder.verify(sessionService, times(1)).dto(session);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenPostSessionsEndpoint_andSessionAlreadyExists_thenConflictIsReturned() {
        when(sessionService.exists(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(true);
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));

        final var inOrder = inOrder(configurationService, sessionService);

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(HttpStatus.CONFLICT, VALIDATION_FAILED, SESSION_ALREADY_EXISTS);

        inOrder.verify(sessionService, times(1)).exists(CLIENT_APP_ID, SESSION_REFERENCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenPostSessionsEndpoint_andSessionConfigDoesNotExist_thenBadRequestIsReturned() {
        when(sessionService.exists(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(false);
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));
        when(configurationService.getConfig(String.valueOf(PME_CONFIGURATION_ID))).thenReturn(null);

        final var inOrder = inOrder(configurationService, sessionService);

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, PME_CONFIG_ID_DOES_NOT_EXIST);

        inOrder.verify(sessionService, times(1)).exists(CLIENT_APP_ID, SESSION_REFERENCE);
        inOrder.verify(configurationService, times(1)).getConfig(String.valueOf(PME_CONFIGURATION_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenPostSessionsEndpoint_andSessionConfigIsNotValid_thenBadRequestIsReturned() {
        when(sessionService.exists(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(false);
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, "AnInvalidConfigurationId");

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, PME_CONFIG_ID_DOES_NOT_EXIST);

        verify(sessionService, times(1)).exists(CLIENT_APP_ID, SESSION_REFERENCE);
    }

    @Test
    void whenPostSessionsEndpoint_andSessionRequestIsInvalid_thenBadRequestIsReturned() {
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));
        doThrow(ControllerDetailException.class).when(validationService).validateSessionRequest(sessionRequest);

        final var inOrder = inOrder(configurationService, sessionService, validationService);

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class);

        inOrder.verify(validationService, times(1)).validateClientId(CLIENT_APP_ID);
        inOrder.verify(validationService, times(1)).validateSessionRequest(sessionRequest);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenPostSessionsEndpoint_andSessionRequestReferenceIsInvalid_thenBadRequestIsReturned() {
        final var sessionRequest = new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));
        doThrow(ControllerDetailException.class).when(validationService).validateSessionReference(sessionRequest);

        final var inOrder = inOrder(configurationService, sessionService, validationService);

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, sessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class);

        inOrder.verify(validationService, times(1)).validateSessionReference(sessionRequest);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenPostSessionsEndpoint_andClientIdIsInvalid_thenBadRequestIsReturned() {
        doThrow(ControllerDetailException.class).when(validationService).validateClientId(CLIENT_APP_ID);

        final var inOrder = inOrder(configurationService, sessionService, validationService);

        assertThatThrownBy(() -> objectUnderTest.createSession(CLIENT_APP_ID, mock(EpmeSessionRequest.class), APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class);

        inOrder.verify(validationService, times(1)).validateClientId(CLIENT_APP_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenGetSessionsEndpoint_andClientIdIsValid_thenSessionIsReturned() {
        final Session session = createSession();
        final EpmeSession epmeSession = new EpmeSession();
        when(sessionService.findByClientId(CLIENT_APP_ID)).thenReturn(Collections.singletonList(session));
        when(sessionService.dto(session)).thenReturn(epmeSession);
        assertThat(objectUnderTest.getSessions(CLIENT_APP_ID, APPLICATION_JSON, null))
                .hasStatus(HttpStatus.OK)
                .hasBody(Collections.singletonList(epmeSession));
    }

    @Test
    void whenGetSessionsEndpoint_andSessionReferenceIsProvided_thenTheSessionReferenceIsUsedToFilter() {
        final Session session = createSession();
        final EpmeSession epmeSession = new EpmeSession();
        when(sessionService.findByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(Collections.singletonList(session));
        when(sessionService.dto(session)).thenReturn(epmeSession);
        assertThat(objectUnderTest.getSessions(CLIENT_APP_ID, APPLICATION_JSON, SESSION_REFERENCE))
                .hasStatus(HttpStatus.OK)
                .hasBody(Collections.singletonList(epmeSession));
    }

    @Test
    void whenGetSessionsEndpoint_andClientIdDoesNotExist_thenEmptyListIsReturned() {
        assertThat(objectUnderTest.getSessions(CLIENT_APP_ID, APPLICATION_JSON, null))
                .hasStatus(HttpStatus.OK)
                .hasBody(new ArrayList<>());
    }

    @Test
    void whenGetSessionByIdEndpoint_andSessionIdExists_theSessionIsReturned() {
        final Session session = createSession();
        final EpmeSession epmeSession = new EpmeSession();
        when(sessionService.findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionService.dto(session)).thenReturn(epmeSession);
        assertThat(objectUnderTest.getSessionById(CLIENT_APP_ID, PME_SESSION_ID, APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .hasBody(epmeSession);
    }

    @Test
    void whenGetSessionByIdEndpoint_andSessionIdDoesNotExist_thenThrowControllerDetailException() {
        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.getSessionById(APPLICATION_JSON, CLIENT_APP_ID, PME_SESSION_ID));

        assertThat(controllerDetailException.getStatus()).isEqualTo(NOT_FOUND);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Provide a valid Client ID and Session ID\"", NOT_FOUND);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Session with provided ID does not exist");
    }

    @Test
    void whenStopSessionByIdEndpoint_andBothSessionIdAndClientIdAreValid_thenAcceptedIsReturned() {
        final var session = createSession();
        final var stopSessionRequest = new EpmeSessionStopRequest(EpmeSessionStopRequest.StatusEnum.STOPPED);

        when(sessionService.findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionService.updateSessionToStopped(PME_SESSION_ID, CLIENT_APP_ID)).thenReturn(1);

        final var inorder = inOrder(sessionService);

        assertThat(objectUnderTest.stopSessionById(CLIENT_APP_ID, PME_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(HttpStatus.ACCEPTED)
                .hasBody(stopSessionRequest);

        inorder.verify(sessionService, times(1)).findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID);
        inorder.verify(sessionService, times(1)).updateSessionToStopped(PME_SESSION_ID, CLIENT_APP_ID);
        inorder.verifyNoMoreInteractions();
    }

    @Test
    void whenStopSessionByIdEndpoint_andClientIdIsInvalid_thenBadRequestIsReturned() {
        final var stopSessionRequest = mock(EpmeSessionStopRequest.class);
        final var exceptionWithDetails = new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CLIENT_ID_NON_NULL);
        doThrow(exceptionWithDetails).when(validationService).validateClientId(null);

        final var inOrder = inOrder(validationService);

        assertThatThrownBy(
                () -> objectUnderTest.stopSessionById(null, PME_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .isInstanceOf(ControllerDetailException.class)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(BAD_REQUEST, VALIDATION_FAILED, CLIENT_ID_NON_NULL);

        inOrder.verify(validationService, times(1)).validateClientId(null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenStopSessionByIdEndpoint_andSessionIdIsInvalid_thenBadRequestIsReturned() {
        final String INVALID_SESSION_ID = "Wrong ID";
        final var stopSessionRequest = mock(EpmeSessionStopRequest.class);
        final var inOrder = inOrder(sessionService);

        final ControllerDetailException exception = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.stopSessionById(CLIENT_APP_ID, INVALID_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON));

        assertThat(exception)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(NOT_FOUND, VALIDATION_FAILED, SESSION_DOES_NOT_EXIST);

        inOrder.verify(sessionService, times(1)).findByClientIdAndSessionId(CLIENT_APP_ID, INVALID_SESSION_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenStopSessionByIdEndpoint_andSessionIsAlreadyStopped_thenConflictIsReturned() {
        final var stopSessionRequest = mock(EpmeSessionStopRequest.class);
        final var inOrder = inOrder(sessionService);
        final var session = createSession();
        session.setStatus(STOPPED);

        when(sessionService.findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));

        final ControllerDetailException exception = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.stopSessionById(CLIENT_APP_ID, PME_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON));

        assertThat(exception)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(CONFLICT, VALIDATION_FAILED, SESSION_IS_STOPPED);

        inOrder.verify(sessionService, times(1)).findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenStopSessionByIdEndpoint_andSessionIsAlreadyFinished_thenConflictIsReturned() {
        final var stopSessionRequest = mock(EpmeSessionStopRequest.class);
        final var inOrder = inOrder(sessionService);
        final var session = createSession();
        session.setStatus(FINISHED);

        when(sessionService.findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));

        final ControllerDetailException exception = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.stopSessionById(CLIENT_APP_ID, PME_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON));

        assertThat(exception)
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(CONFLICT, VALIDATION_FAILED, SESSION_IS_STOPPED);

        inOrder.verify(sessionService, times(1)).findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenStopSessionByIdEndpoint_andSessionIsNotUpdated_thenInternalServerErrorIsReturned() {
        final var stopSessionRequest = mock(EpmeSessionStopRequest.class);
        final var session = createSession();
        final var inOrder = inOrder(sessionService);

        when(sessionService.findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionService.updateSessionToStopped(PME_SESSION_ID, CLIENT_APP_ID)).thenReturn(0);

        assertThatThrownBy(
                () -> objectUnderTest.stopSessionById(CLIENT_APP_ID, PME_SESSION_ID, stopSessionRequest, APPLICATION_JSON, APPLICATION_JSON))
                .extracting(STATUS_FIELD, REASON_FIELD)
                .containsExactly(INTERNAL_SERVER_ERROR, SESSION_UPDATE_FAILED);

        inOrder.verify(sessionService, times(1)).findByClientIdAndSessionId(CLIENT_APP_ID, PME_SESSION_ID);
        inOrder.verify(sessionService, times(1)).updateSessionToStopped(PME_SESSION_ID, CLIENT_APP_ID);
        inOrder.verifyNoMoreInteractions();
    }

    private Session createSession() {
        return new Session(PME_SESSION_ID,
                CLIENT_APP_ID,
                SESSION_REFERENCE,
                18,
                new SessionConfiguration(),
                CREATED,
                timestamp,
                timestamp,
                timestamp);
    }
}
