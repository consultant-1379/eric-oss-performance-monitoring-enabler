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

import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_ASSOCIATED_WITH_SESSION_DELETE;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_ASSOCIATED_WITH_SESSION_UPDATE;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_ID_CANNOT_BE_CHANGED;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_NAME_ALREADY_EXISTS;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_NAME_CANNOT_BE_CHANGED;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_USED;
import static com.ericsson.oss.apps.util.Constants.INTERNAL_SERVER_ERROR;
import static com.ericsson.oss.apps.util.Constants.MAX_CONFIGURATIONS_EXCEEDED;
import static com.ericsson.oss.apps.util.Constants.PERSISTENCE_FAILED;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_A_VALID_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.VERSION;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.oss.apps.api.ConfigurationsApi;
import com.ericsson.oss.apps.api.model.EpmeConfiguration;
import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeConfigurationUpdate;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.config.PersistenceConfig;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.KpiConfiguration;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.ThresholdType;
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
public class ConfigurationController implements ConfigurationsApi {
    private static final String FAILED_TO_CREATE_CONFIG_FOR = "Failed to create configuration for '{}': {}";
    private static final String FAILED_TO_UPDATE_CONFIG_FOR = "Failed to update configuration for '{}': {}";

    private final SessionService sessionService;
    private final ConfigurationService configurationService;
    private final ValidationService validationService;
    private final PersistenceConfig persistenceConfig;

    @Override
    @Timed
    public ResponseEntity<List<EpmeConfiguration>> getConfigurations() {
        log.info("GET {}/configurations REST interface is invoked.", VERSION);
        final List<EpmeConfiguration> epmeConfigurationList = configurationService.findAll()
                .stream()
                .map(configurationService::dto)
                .toList();

        return new ResponseEntity<>(epmeConfigurationList, HttpStatus.OK);
    }

    @Override
    @Timed
    public ResponseEntity<EpmeConfiguration> getConfigurationById(final String configurationId, final String accept) {
        log.info("GET {}/configurations/ REST interface is invoked.", VERSION);
        final SessionConfiguration sessionConfiguration = configurationService.getConfig(configurationId);
        if (Objects.nonNull(sessionConfiguration)) {
            return new ResponseEntity<>(configurationService.dto(sessionConfiguration), HttpStatus.OK);
        } else {
            log.error("Failed to get configuration with ID '{}'", configurationId);
            throw ControllerDetailException.builder()
                    .withStatus(HttpStatus.NOT_FOUND)
                    .withReason(PROVIDE_A_VALID_CONFIGURATION_ID)
                    .withDetail(CONFIGURATION_DOES_NOT_EXIST)
                    .build();
        }
    }

    @Override
    @Timed
    public ResponseEntity<Void> deleteConfiguration(final String configurationId, final String accept) {
        log.info("DELETE {}/configurations REST interface is invoked.", VERSION);

        try {
            final long id = Long.parseLong(configurationId);

            if (sessionService.isSessionConfigurationInUse(id)) {
                final ControllerDetailException controllerDetailException = new ControllerDetailException(HttpStatus.CONFLICT,
                        CONFIGURATION_ASSOCIATED_WITH_SESSION_DELETE,
                        CONFIGURATION_USED);
                log.error("Failed to delete configuration with ID '{}'", configurationId, controllerDetailException);
                throw controllerDetailException;
            }
            configurationService.deleteSessionConfigurationById(id);
        } catch (final IllegalArgumentException e) {
            throw buildExceptionForInvalidConfigurationId(e, HttpStatus.NOT_FOUND, CONFIGURATION_DOES_NOT_EXIST);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

    public ControllerDetailException buildExceptionForInvalidConfigurationId(final Exception e, final HttpStatus httpStatus, final String message) {
        log.error(message, e);
        return new ControllerDetailException(httpStatus, PROVIDE_A_VALID_CONFIGURATION_ID,
                message);
    }

    @Override
    @Timed
    public ResponseEntity<EpmeConfiguration> createConfiguration(final EpmeConfigurationRequest epmeConfigurationRequest) {
        log.info("POST {}/configurations REST interface is invoked.", VERSION);

        try {
            validationService.validateConfigurationRequest(epmeConfigurationRequest);
        } catch (final ControllerDetailException e) {
            log.error(FAILED_TO_CREATE_CONFIG_FOR, epmeConfigurationRequest.getName(), VALIDATION_FAILED, e);
            throw e;
        }

        try {
            if (configurationService.exists(epmeConfigurationRequest.getName())) {
                log.error(FAILED_TO_CREATE_CONFIG_FOR,
                        epmeConfigurationRequest.getName(), CONFIGURATION_NAME_ALREADY_EXISTS);
                throw new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, CONFIGURATION_NAME_ALREADY_EXISTS);
            }

            if (configurationService.count() > persistenceConfig.getMaxConfigurations()) {
                log.error(FAILED_TO_CREATE_CONFIG_FOR,
                        epmeConfigurationRequest.getName(), MAX_CONFIGURATIONS_EXCEEDED + persistenceConfig.getMaxConfigurations());
                throw new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                        MAX_CONFIGURATIONS_EXCEEDED + persistenceConfig.getMaxConfigurations());
            }

            final SessionConfiguration sessionConfiguration = configurationService.createSessionConfiguration(epmeConfigurationRequest);
            return new ResponseEntity<>(configurationService.dto(sessionConfiguration), HttpStatus.CREATED);
        } catch (final DataAccessException e) {
            log.error("Failed to create session Configuration for {}", epmeConfigurationRequest.getName(), e);
            throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, PERSISTENCE_FAILED); //NOPMD
        }
    }

    @Override
    @Timed
    public ResponseEntity<EpmeConfiguration> updateConfiguration(final String configurationId,
            final EpmeConfigurationUpdate epmeConfigurationUpdate, final String accept, final String contentType) {
        log.info("PUT {}/configurations REST interface is invoked.", VERSION);

        try {
            validationService.validateConfigurationUpdate(epmeConfigurationUpdate);
        } catch (final ControllerDetailException e) {
            log.error(FAILED_TO_UPDATE_CONFIG_FOR, epmeConfigurationUpdate.getName(), VALIDATION_FAILED, e);
            throw e;
        }

        try {
            final SessionConfiguration existingSessionConfiguration = configurationService.getConfig(configurationId);
            if (Objects.isNull(existingSessionConfiguration)) {
                log.warn("Tried to update a Configuration that does not exist for ID '{}'.", configurationId);
                throw new IllegalArgumentException();
            }
            checkOptionalData(configurationId, epmeConfigurationUpdate, existingSessionConfiguration);
            if (sessionService.isSessionConfigurationInUse(existingSessionConfiguration.getId())) {
                final ControllerDetailException controllerDetailException = new ControllerDetailException(HttpStatus.CONFLICT,
                        CONFIGURATION_ASSOCIATED_WITH_SESSION_UPDATE,
                        CONFIGURATION_USED);
                log.error("Failed to update configuration with ID '{}'", configurationId, controllerDetailException);
                throw controllerDetailException;
            }

            final SessionConfiguration sessionConfig = new SessionConfiguration(epmeConfigurationUpdate.getName(),
                    epmeConfigurationUpdate.getWeekendDays().toString());
            final List<KpiConfiguration> kpiConfigs = epmeConfigurationUpdate.getFixedThresholdKpis().stream()
                    .map(this::createKpiConfiguration)
                    .collect(Collectors.toList());
            sessionConfig.setKpiConfigs(kpiConfigs);
            final SessionConfiguration sessionConfiguration = configurationService.updateSessionConfigurationById(configurationId, sessionConfig);
            return new ResponseEntity<>(configurationService.dto(sessionConfiguration), HttpStatus.OK);
        } catch (final IllegalArgumentException e) {
            throw buildExceptionForInvalidConfigurationId(e, HttpStatus.NOT_FOUND, CONFIGURATION_DOES_NOT_EXIST);
        }
    }

    private void checkOptionalData(final String configurationId, final EpmeConfigurationUpdate epmeConfigurationUpdate,
            final SessionConfiguration existingSessionConfiguration) {
        if (Objects.nonNull(epmeConfigurationUpdate.getId()) &&
                !configurationId.equals(epmeConfigurationUpdate.getId())) {
            log.error(FAILED_TO_UPDATE_CONFIG_FOR,
                    epmeConfigurationUpdate.getName(), CONFIGURATION_ID_CANNOT_BE_CHANGED);
            throw new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, CONFIGURATION_ID_CANNOT_BE_CHANGED);
        }
        if (Objects.nonNull(epmeConfigurationUpdate.getName()) &&
                !existingSessionConfiguration.getName().equals(epmeConfigurationUpdate.getName())) {
            log.error(FAILED_TO_UPDATE_CONFIG_FOR,
                    epmeConfigurationUpdate.getName(), CONFIGURATION_NAME_CANNOT_BE_CHANGED);
            throw new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, CONFIGURATION_NAME_CANNOT_BE_CHANGED);
        }
    }

    private KpiConfiguration createKpiConfiguration(final EpmeFixed epmeFixed) {
        final KpiConfiguration kpiConfiguration = new KpiConfiguration();
        kpiConfiguration.setKpiName(epmeFixed.getKpiName());
        kpiConfiguration.setMonitor(true);
        kpiConfiguration.setThresholdType(ThresholdType.FIXED);
        kpiConfiguration.setFixedThresholdValue(epmeFixed.getFixedThreshold());
        return kpiConfiguration;
    }
}
