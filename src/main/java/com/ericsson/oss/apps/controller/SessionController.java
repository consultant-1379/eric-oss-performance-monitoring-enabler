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

import static com.ericsson.oss.apps.api.model.EpmeSessionStopRequest.StatusEnum.STOPPED;
import static com.ericsson.oss.apps.util.Constants.HOURLY_EXECUTION_WILL_COMPLETE;
import static com.ericsson.oss.apps.util.Constants.PME_CONFIG_ID_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_A_VALID_CLIENT_ID_AND_SESSION_ID;
import static com.ericsson.oss.apps.util.Constants.SESSION_ALREADY_EXISTS;
import static com.ericsson.oss.apps.util.Constants.SESSION_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.SESSION_IS_STOPPED;
import static com.ericsson.oss.apps.util.Constants.SESSION_UPDATE_FAILED;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.VERSION;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.oss.apps.api.SessionsApi;
import com.ericsson.oss.apps.api.model.EpmeSession;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.api.model.EpmeSessionStopRequest;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.service.ConfigurationService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.ValidationService;

import io.micrometer.core.annotation.Timed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(VERSION)
@RequiredArgsConstructor
public class SessionController implements SessionsApi {
    private static final String FAILED_TO_CREATE_SESSION = "Failed to create Session: {}";
    private static final String FAILED_TO_CREATE_SESSION_FOR = "Failed to create Session for '{}': {}";
    private static final String FAILED_TO_STOP_SESSION = "Failed to stop Session: {}";
    private static final String SESSION_SUCCESSFULLY_UPDATED = "Session was successfully updated: {}";
    private final ConfigurationService configurationService;
    private final SessionService sessionService;
    private final ValidationService validationService;

    @Override
    @Timed
    public ResponseEntity<EpmeSession> createSession(final String clientId, final EpmeSessionRequest sessionRequest,
            final String accept, final String contentType) {
        log.info("POST {}/sessions REST interface is invoked.", VERSION);

        try {
            validationService.validateSessionReference(sessionRequest);
        } catch (final ControllerDetailException e) {
            log.error(FAILED_TO_CREATE_SESSION, VALIDATION_FAILED, e);
            throw e;
        }

        try {
            validationService.validateClientId(clientId);
            validationService.validateSessionRequest(sessionRequest);
        } catch (final ControllerDetailException e) {
            log.error(FAILED_TO_CREATE_SESSION_FOR, sessionRequest.getSessionReference(), VALIDATION_FAILED, e);
            throw e;
        }

        if (sessionService.exists(clientId, sessionRequest.getSessionReference())) {
            log.error(FAILED_TO_CREATE_SESSION_FOR, sessionRequest.getSessionReference(), SESSION_ALREADY_EXISTS);
            throw new ControllerDetailException(HttpStatus.CONFLICT, VALIDATION_FAILED, SESSION_ALREADY_EXISTS);
        }

        final var sessionConfig = getConfig(sessionRequest);
        if (Objects.isNull(sessionConfig)) {
            log.error(FAILED_TO_CREATE_SESSION_FOR, sessionRequest.getSessionReference(), PME_CONFIG_ID_DOES_NOT_EXIST);
            throw new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, PME_CONFIG_ID_DOES_NOT_EXIST);
        }

        final var session = sessionService.createSession(clientId, sessionRequest, sessionConfig);

        return new ResponseEntity<>(sessionService.dto(session), HttpStatus.CREATED);
    }

    @Override
    @Timed
    public ResponseEntity<List<EpmeSession>> getSessions(final String clientId, final String accept, final String sessionReference) {
        log.info("GET {}/sessions/ REST interface is invoked.", VERSION);
        if (Objects.isNull(sessionReference)) {
            final List<EpmeSession> epmeSessions = sessionService.findByClientId(clientId)
                    .stream()
                    .map(sessionService::dto)
                    .toList();
            return new ResponseEntity<>(epmeSessions, HttpStatus.OK);
        } else {
            final List<EpmeSession> epmeSessions = sessionService.findByClientIdAndSessionReference(clientId, sessionReference)
                    .stream()
                    .map(sessionService::dto)
                    .toList();
            return new ResponseEntity<>(epmeSessions, HttpStatus.OK);
        }
    }

    @Override
    @Timed
    public ResponseEntity<EpmeSession> getSessionById(final String clientId, final String sessionId, final String accept) {
        log.info("GET {}/sessions/{sessionId} REST interface is invoked.", VERSION);
        final Optional<Session> sessionOptional = sessionService.findByClientIdAndSessionId(clientId, sessionId);
        if (sessionOptional.isEmpty()) {
            log.error("Failed to get session with provided Client Id and Sessions Id");
            throw ControllerDetailException.builder()
                    .withStatus(HttpStatus.NOT_FOUND)
                    .withReason(PROVIDE_A_VALID_CLIENT_ID_AND_SESSION_ID)
                    .withDetail(SESSION_DOES_NOT_EXIST)
                    .build();
        } else {
            return new ResponseEntity<>(sessionService.dto(sessionOptional.get()), HttpStatus.OK);
        }
    }

    @Override
    @Timed
    public ResponseEntity<EpmeSessionStopRequest> stopSessionById(final String clientId, final String sessionId,
            final EpmeSessionStopRequest epmeSessionStopRequest, final String accept, final String contentType) {
        log.info("PUT {}/sessions/{sessionId} REST interface is invoked.", VERSION);
        try {
            validationService.validateClientId(clientId);
            validationService.validateSessionStopRequest(epmeSessionStopRequest);
        } catch (final ControllerDetailException e) {
            log.error(FAILED_TO_STOP_SESSION, VALIDATION_FAILED, e);
            throw e;
        }

        final Optional<Session> optionalSession = sessionService.findByClientIdAndSessionId(clientId, sessionId);

        if (optionalSession.isEmpty()) {
            log.error(FAILED_TO_STOP_SESSION, SESSION_DOES_NOT_EXIST);
            throw new ControllerDetailException(HttpStatus.NOT_FOUND, VALIDATION_FAILED, SESSION_DOES_NOT_EXIST);
        }

        final Session session = optionalSession.get();
        final SessionStatus sessionStatus = session.getStatus();

        if (sessionStatus.equals(SessionStatus.STOPPED) || sessionStatus.equals(SessionStatus.FINISHED)) {
            log.error(FAILED_TO_STOP_SESSION, SESSION_IS_STOPPED);
            throw new ControllerDetailException(HttpStatus.CONFLICT, VALIDATION_FAILED, SESSION_IS_STOPPED);
        }

        final int updateCount = sessionService.updateSessionToStopped(sessionId, clientId);
        if (updateCount > 0) {
            log.info(SESSION_SUCCESSFULLY_UPDATED, HOURLY_EXECUTION_WILL_COMPLETE);
            return new ResponseEntity<>(new EpmeSessionStopRequest(STOPPED), HttpStatus.ACCEPTED);
        }

        throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, SESSION_UPDATE_FAILED);
    }

    private SessionConfiguration getConfig(final EpmeSessionRequest sessionRequest) {
        try {
            return configurationService.getConfig(sessionRequest.getPmeConfigId());
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
