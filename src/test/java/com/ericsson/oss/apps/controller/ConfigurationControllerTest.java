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
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_ID_CANNOT_BE_CHANGED;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_NAME_ALREADY_EXISTS;
import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_NAME_CANNOT_BE_CHANGED;
import static com.ericsson.oss.apps.util.Constants.CONFIG_NAME_INVALID_PATTERN;
import static com.ericsson.oss.apps.util.Constants.INTERNAL_SERVER_ERROR;
import static com.ericsson.oss.apps.util.Constants.MAX_CONFIGURATIONS_EXCEEDED;
import static com.ericsson.oss.apps.util.Constants.PERSISTENCE_FAILED;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_A_VALID_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.WEEKEND_DAYS_NON_NULL;
import static com.ericsson.oss.apps.util.TestConstants.APPLICATION_JSON;
import static com.ericsson.oss.apps.util.TestConstants.WEEKEND_DAYS;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.api.model.EpmeConfiguration;
import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeConfigurationUpdate;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.config.PersistenceConfig;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.service.ConfigurationService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.ValidationService;

/**
 * Unit tests for {@link ConfigurationController} class
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationControllerTest {

    private static final String CONFIGURATION_ID = "1";
    private static final String CONFIGURATION_NAME = "configuration_name";

    private static final double ONE_MBPS = 1000;
    private static final int MAX_CONFIGURATIONS = -1;

    @Mock
    private SessionService sessionService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private ValidationService validationService;

    @Mock
    private PersistenceConfig persistenceConfig;

    @InjectMocks
    private ConfigurationController objectUnderTest;

    @Test
    void whenGetConfigurationsEndpoint_thenOkIsReturned() {
        assertThat(objectUnderTest.getConfigurations())
                .hasStatus(HttpStatus.OK)
                .hasBody();
    }

    @Test
    void whenGetConfigurationByIdEndpoint_andConfigurationDoesNotExist_thenThrowControllerDetailException() {
        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.getConfigurationById(CONFIGURATION_ID, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(NOT_FOUND);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Provide a valid Configuration ID\"", NOT_FOUND);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Configuration with provided ID does not exist");
    }

    @Test
    void whenGetConfigurationByIdEndpoint_andConfigurationExists_thenOkIsReturned() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        final EpmeConfiguration epmeConfiguration = new EpmeConfiguration();
        sessionConfiguration.setWeekendDays(WEEKEND_DAYS);
        when(configurationService.getConfig(CONFIGURATION_ID)).thenReturn(sessionConfiguration);
        when(configurationService.dto(sessionConfiguration)).thenReturn(epmeConfiguration);

        assertThat(objectUnderTest.getConfigurationById(CONFIGURATION_ID, APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .hasBody(epmeConfiguration);
    }

    @Test
    void whenPostConfigurationEndpoint_andConfigurationIsValid_thenCreatedIsReturned() {
        final var sessionConfigurationRequest = mock(EpmeConfigurationRequest.class);
        final var sessionConfigurationDto = mock(EpmeConfiguration.class);
        final var sessionConfiguration = mock(SessionConfiguration.class);
        when(configurationService.exists(nullable(String.class))).thenReturn(false);
        when(persistenceConfig.getMaxConfigurations()).thenReturn(1000);
        when(configurationService.createSessionConfiguration(sessionConfigurationRequest))
                .thenReturn(sessionConfiguration);
        when(configurationService.dto(sessionConfiguration)).thenReturn(sessionConfigurationDto);
        doNothing().when(validationService).validateConfigurationRequest(any(EpmeConfigurationRequest.class));

        final var inOrder = inOrder(configurationService, validationService, persistenceConfig);

        assertThat(objectUnderTest.createConfiguration(sessionConfigurationRequest))
                .hasStatus(HttpStatus.CREATED)
                .hasBody(sessionConfigurationDto);
        inOrder.verify(validationService, times(1)).validateConfigurationRequest(any(EpmeConfigurationRequest.class));
        inOrder.verify(configurationService, times(1)).exists(nullable(String.class));
        inOrder.verify(persistenceConfig, times(1)).getMaxConfigurations();
        inOrder.verify(configurationService, times(1)).createSessionConfiguration(sessionConfigurationRequest);
        inOrder.verify(configurationService, times(1)).dto(sessionConfiguration);
    }

    @Test
    void whenPostConfigurationEndpoint_andConfigurationNameAlreadyUsed_thenControllerDetailExceptionThrown() {
        when(configurationService.exists(nullable(String.class))).thenReturn(true);
        doNothing().when(validationService).validateConfigurationRequest(any(EpmeConfigurationRequest.class));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.createConfiguration(mock(EpmeConfigurationRequest.class)));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIGURATION_NAME_ALREADY_EXISTS);
    }

    @Test
    void whenPostConfigurationEndpoint_andConfigurationValidationFails_thenControllerDetailExceptionThrown() {
        doThrow(new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, CONFIG_NAME_INVALID_PATTERN))
                .when(validationService).validateConfigurationRequest(any(EpmeConfigurationRequest.class));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.createConfiguration(mock(EpmeConfigurationRequest.class)));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIG_NAME_INVALID_PATTERN);
    }

    @Test
    void whenPostConfigurationEndpoint_andMaxConfigurationsHaveAlreadyBeenCreated_thenControllerDetailExceptionThrown() {
        when(configurationService.exists(nullable(String.class))).thenReturn(false);
        when(persistenceConfig.getMaxConfigurations()).thenReturn(MAX_CONFIGURATIONS);

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.createConfiguration(mock(EpmeConfigurationRequest.class)));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(MAX_CONFIGURATIONS_EXCEEDED + MAX_CONFIGURATIONS);
    }

    @Test
    public void whenPostConfigurationEndpoint_andDataAccessExceptionOccurs_thenControllerDetailExceptionIsThrown() {
        when(configurationService.exists(nullable(String.class))).thenThrow(new DataAccessResourceFailureException(""));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.createConfiguration(mock(EpmeConfigurationRequest.class)));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getReason()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getDetail()).isEqualTo(PERSISTENCE_FAILED);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationIsValid_thenOkIsReturned() {

        final EpmeConfigurationUpdate epmeConfigurationUpdate = new EpmeConfigurationUpdate(EpmeWeekendDays.SATURDAY_SUNDAY,
                createValidFixedThreadholds());
        epmeConfigurationUpdate.setId(CONFIGURATION_ID);
        epmeConfigurationUpdate.setName(CONFIGURATION_NAME);

        final var sessionConfiguration = mock(SessionConfiguration.class);
        final EpmeConfiguration epmeConfiguration = new EpmeConfiguration();
        sessionConfiguration.setWeekendDays(WEEKEND_DAYS);
        doNothing().when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        when(sessionConfiguration.getName()).thenReturn(CONFIGURATION_NAME);
        when(configurationService.getConfig(any(String.class)))
                .thenReturn(sessionConfiguration);
        when(configurationService.updateSessionConfigurationById(any(String.class), any(SessionConfiguration.class)))
                .thenReturn(sessionConfiguration);
        when(configurationService.dto(sessionConfiguration)).thenReturn(epmeConfiguration);
        final var inOrder = inOrder(validationService, sessionConfiguration, configurationService);

        assertThat(objectUnderTest.updateConfiguration(CONFIGURATION_ID, epmeConfigurationUpdate, APPLICATION_JSON, APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .hasBody(epmeConfiguration);

        inOrder.verify(validationService, times(1)).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        inOrder.verify(configurationService, times(1)).getConfig(CONFIGURATION_ID);
        inOrder.verify(sessionConfiguration, times(1)).getName();
        inOrder.verify(configurationService, times(1)).updateSessionConfigurationById(eq(CONFIGURATION_ID), any(SessionConfiguration.class));
        inOrder.verify(configurationService, times(1)).dto(sessionConfiguration);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationValidationFails_thenControllerDetailExceptionThrown() {
        doThrow(new ControllerDetailException(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, WEEKEND_DAYS_NON_NULL))
                .when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateConfiguration(CONFIGURATION_ID, mock(EpmeConfigurationUpdate.class), APPLICATION_JSON, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(WEEKEND_DAYS_NON_NULL);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationDoesNotExist_thenControllerDetailExceptionThrown() {
        doNothing().when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        when(configurationService.getConfig(CONFIGURATION_ID)).thenReturn(null);

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateConfiguration(CONFIGURATION_ID, mock(EpmeConfigurationUpdate.class), APPLICATION_JSON, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(NOT_FOUND);
        assertThat(controllerDetailException.getReason()).isEqualTo(PROVIDE_A_VALID_CONFIGURATION_ID);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIGURATION_DOES_NOT_EXIST);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationIdInPayloadDoesNotMatch_thenControllerDetailExceptionThrown() {
        final var sessionConfigurationUpdate = mock(EpmeConfigurationUpdate.class);
        final var existingSessionConfiguration = mock(SessionConfiguration.class);

        doNothing().when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        when(configurationService.getConfig(CONFIGURATION_ID)).thenReturn(existingSessionConfiguration);
        when(sessionConfigurationUpdate.getId()).thenReturn("99999");

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateConfiguration(CONFIGURATION_ID, sessionConfigurationUpdate, APPLICATION_JSON, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIGURATION_ID_CANNOT_BE_CHANGED);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationNameInPayloadDoesNotMatch_thenControllerDetailExceptionThrown() {
        final var sessionConfigurationUpdate = mock(EpmeConfigurationUpdate.class);
        final var existingSessionConfiguration = mock(SessionConfiguration.class);

        doNothing().when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        when(configurationService.getConfig(CONFIGURATION_ID)).thenReturn(existingSessionConfiguration);
        when(sessionConfigurationUpdate.getId()).thenReturn(CONFIGURATION_ID);
        when(sessionConfigurationUpdate.getName()).thenReturn("name_different_than_existing_name");
        when(existingSessionConfiguration.getName()).thenReturn(CONFIGURATION_NAME);

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateConfiguration(CONFIGURATION_ID, sessionConfigurationUpdate, APPLICATION_JSON, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controllerDetailException.getReason()).isEqualTo(VALIDATION_FAILED);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIGURATION_NAME_CANNOT_BE_CHANGED);
    }

    @Test
    void whenPutConfigurationEndpoint_andConfigurationExists_andConfigurationIsUsedInASession_thenThrowControllerDetailException() {
        doNothing().when(validationService).validateConfigurationUpdate(any(EpmeConfigurationUpdate.class));
        when(configurationService.getConfig(CONFIGURATION_ID)).thenReturn(mock(SessionConfiguration.class));
        when(sessionService.isSessionConfigurationInUse(anyLong())).thenReturn(true);

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateConfiguration(CONFIGURATION_ID, mock(EpmeConfigurationUpdate.class), APPLICATION_JSON, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(CONFLICT);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Only Configurations not associated with a Session can be updated\"",
                CONFLICT);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Configuration with provided ID is used in a Session");
    }

    @Test
    void whenDeleteConfigurationEndpoint_andConfigurationExists_andConfigurationIsNotUsedInASession_thenNoContentIsReturned() {
        assertThat(objectUnderTest.deleteConfiguration(CONFIGURATION_ID, APPLICATION_JSON))
                .hasStatus(HttpStatus.NO_CONTENT)
                .doesNotHaveBody();
    }

    @Test
    void whenDeleteConfigurationEndpoint_andConfigurationExists_andConfigurationIsUsedInASession_thenThrowControllerDetailException() {
        when(sessionService.isSessionConfigurationInUse(anyLong())).thenReturn(true);

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.deleteConfiguration(CONFIGURATION_ID, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(CONFLICT);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Only Configurations not associated with a Session can be deleted\"",
                CONFLICT);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Configuration with provided ID is used in a Session");
    }

    @Test
    void whenDeleteConfigurationEndpoint_andConfigurationDoesNotExist_thenNotFoundIsReturned() {
        doThrow(new IllegalArgumentException()).when(configurationService).deleteSessionConfigurationById(Long.parseLong(CONFIGURATION_ID));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.deleteConfiguration(CONFIGURATION_ID, APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(NOT_FOUND);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Provide a valid Configuration ID\"", NOT_FOUND);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Configuration with provided ID does not exist");

    }

    @Test
    void whenDeleteConfigurationEndpoint_andConfigurationIdIsNonNumeric_thenExceptionIsThrown() {
        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.deleteConfiguration("one", APPLICATION_JSON));

        assertThat(controllerDetailException.getStatus()).isEqualTo(NOT_FOUND);
        assertThat(controllerDetailException.getMessage()).isEqualTo("%s \"Provide a valid Configuration ID\"", NOT_FOUND);
        assertThat(controllerDetailException.getDetail()).isEqualTo("Configuration with provided ID does not exist");

    }

    private Set<EpmeFixed> createValidFixedThreadholds() {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed("avg_dl_mac_drb_throughput_hourly", ONE_MBPS));
        return fixedThresholdKpis;
    }
}
