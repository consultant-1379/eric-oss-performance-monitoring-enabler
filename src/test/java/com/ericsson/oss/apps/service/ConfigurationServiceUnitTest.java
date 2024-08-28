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

import static com.ericsson.oss.apps.util.Constants.CONFIGURATION_DOES_NOT_EXIST;
import static com.ericsson.oss.apps.util.Constants.DELETE_FAILED;
import static com.ericsson.oss.apps.util.Constants.GET_FAILED;
import static com.ericsson.oss.apps.util.Constants.INTERNAL_SERVER_ERROR;
import static com.ericsson.oss.apps.util.Constants.PROVIDE_A_VALID_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.Constants.UPDATE_FAILED;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_CONFIGURATION;
import static com.ericsson.oss.apps.util.TestConstants.WEEKEND_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.KpiConfiguration;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.ThresholdType;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;

@ExtendWith(MockitoExtension.class)
public class ConfigurationServiceUnitTest {

    private static final String SOME_KPI_NAME = "some_kpi_name";
    private static final double SOME_KPI_VALUE_AS_DOUBLE = 98.9D;
    private static final Long CONFIGURATION_ID = 1L;

    @Mock
    private SessionConfigurationRepository sessionConfigurationRepository;

    @InjectMocks
    private ConfigurationService objectUnderTest;

    @Test
    public void whenFindAllIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        when(sessionConfigurationRepository.findAll()).thenReturn(new ArrayList<>(List.of(sessionConfiguration)));

        final List<SessionConfiguration> sessionConfigurationList = objectUnderTest.findAll();

        assertThat(sessionConfigurationList.size()).isEqualTo(1);
        verify(sessionConfigurationRepository, times(1)).findAll();
    }

    @Test
    public void whenFindAllIsCalled_andDataAccessExceptionOccurs_thenControllerDetailExceptionIsThrown() {
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).findAll();

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.findAll());

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getReason()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getDetail()).isEqualTo(GET_FAILED);
    }

    @Test
    public void whenGetConfigIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        when(sessionConfigurationRepository.findById(CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfiguration));

        objectUnderTest.getConfig(String.valueOf(CONFIGURATION_ID));

        verify(sessionConfigurationRepository, times(1)).findById(CONFIGURATION_ID);
    }

    @Test
    public void wheGetConfigIsCalled_andDataAccessExceptionOccurs_thenControllerDetailExceptionIsThrown() {
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).findById(any());

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.getConfig(String.valueOf(CONFIGURATION_ID)));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getReason()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getDetail()).isEqualTo(GET_FAILED);
    }

    @Test
    public void wheGetConfigIsCalled_andConfigurationIdIsNonNumeric_thenControllerDetailExceptionIsThrown() {
        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.getConfig("one"));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controllerDetailException.getReason()).isEqualTo(PROVIDE_A_VALID_CONFIGURATION_ID);
        assertThat(controllerDetailException.getDetail()).isEqualTo(CONFIGURATION_DOES_NOT_EXIST);
    }

    @Test
    public void whenDeleteSessionConfigurationIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        when(sessionConfigurationRepository.findById(CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfiguration));

        objectUnderTest.deleteSessionConfigurationById(CONFIGURATION_ID);

        verify(sessionConfigurationRepository, times(1)).deleteById(CONFIGURATION_ID);
    }

    @Test
    public void whenDeleteSessionConfigurationIsCalled_andDataAccessExceptionOccurs_thenControllerDetailExceptionIsThrown() {
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).deleteById(any());
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();

        when(sessionConfigurationRepository.findById(CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfiguration));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.deleteSessionConfigurationById(CONFIGURATION_ID));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getReason()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getDetail()).isEqualTo(DELETE_FAILED);
    }

    @Test
    public void whenConfigurationIsDeleted_andConfigurationDoesNotExist_thenExceptionIsThrown() {
        assertThatThrownBy(() -> objectUnderTest.deleteSessionConfigurationById(CONFIGURATION_ID)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void whenCreateSessionConfigurationIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        final EpmeConfigurationRequest epmeConfigurationRequest = createEpmeConfigurationRequest();
        final var captor = ArgumentCaptor.forClass(SessionConfiguration.class);
        when(sessionConfigurationRepository.save(captor.capture()))
                .thenReturn(null);

        objectUnderTest.createSessionConfiguration(epmeConfigurationRequest);

        assertThat(captor.getValue())
                .extracting("name", "weekendDays")
                .containsExactly(
                        SESSION_CONFIGURATION,
                        EpmeWeekendDays.SATURDAY_SUNDAY.toString());
        assertThat(captor.getValue())
                .extracting("kpiConfigs").asList().element(0)
                .extracting("kpiName", "fixedThresholdValue").containsExactly(SOME_KPI_NAME, SOME_KPI_VALUE_AS_DOUBLE);

        verify(sessionConfigurationRepository, times(1)).save(captor.getValue());
    }

    @Test
    public void whenGetDtoForSessionConfiguration_thenEpmeConfigurationIsCorrectlyConstructed() {
        final SessionConfiguration sessionConfiguration = createSessionConfiguration();
        final var configurationDto = objectUnderTest.dto(sessionConfiguration);

        assertThat(configurationDto)
                .extracting("id", "name", "weekendDays")
                .containsExactly(
                        "0",
                        SESSION_CONFIGURATION,
                        EpmeWeekendDays.SATURDAY_SUNDAY);
        assertThat(configurationDto.getFixedThresholdKpis().stream().toList())
                .element(0)
                .extracting("kpiName", "fixedThreshold")
                .containsExactly(SOME_KPI_NAME, SOME_KPI_VALUE_AS_DOUBLE);
    }

    @Test
    public void whenExistsByNameIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        when(sessionConfigurationRepository.existsByName(SESSION_CONFIGURATION)).thenReturn(true);
        assertThat(objectUnderTest.exists(SESSION_CONFIGURATION)).isTrue();
        verify(sessionConfigurationRepository, times(1)).existsByName(SESSION_CONFIGURATION);
    }

    @Test
    public void whenCountIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        when(sessionConfigurationRepository.count()).thenReturn(1L);
        assertThat(objectUnderTest.count()).isEqualTo(1L);
        verify(sessionConfigurationRepository, times(1)).count();
    }

    @Test
    public void whenUpdateSessionConfigurationIsCalled_thenSessionConfigurationRepositoryIsCalledCorrectly() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        when(sessionConfigurationRepository.findById(CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfiguration));

        objectUnderTest.updateSessionConfigurationById(CONFIGURATION_ID.toString(), sessionConfiguration);

        verify(sessionConfigurationRepository, times(1)).save(sessionConfiguration);
    }

    @Test
    public void whenUpdateSessionConfigurationIsCalled_andDataAccessExceptionOccurs_thenControllerDetailExceptionIsThrown() {
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).save(any());
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        when(sessionConfigurationRepository.findById(CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfiguration));

        final ControllerDetailException controllerDetailException = (ControllerDetailException) catchThrowable(
                () -> objectUnderTest.updateSessionConfigurationById(CONFIGURATION_ID.toString(), sessionConfiguration));

        assertThat(controllerDetailException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getReason()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(controllerDetailException.getDetail()).isEqualTo(UPDATE_FAILED);
    }

    @Test
    public void whenUpdateConfigurationIsCalled_andConfigurationDoesNotExist_thenExceptionIsThrown() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        assertThatThrownBy(() -> objectUnderTest.updateSessionConfigurationById(CONFIGURATION_ID.toString(), sessionConfiguration))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private EpmeConfigurationRequest createEpmeConfigurationRequest() {
        final Set<EpmeFixed> epmeFixedKpis = new HashSet<>();
        epmeFixedKpis.add(new EpmeFixed(SOME_KPI_NAME, SOME_KPI_VALUE_AS_DOUBLE));
        return new EpmeConfigurationRequest(
                SESSION_CONFIGURATION, EpmeWeekendDays.SATURDAY_SUNDAY, epmeFixedKpis);
    }

    private SessionConfiguration createSessionConfiguration() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration(
                SESSION_CONFIGURATION,
                WEEKEND_DAYS);
        final List<KpiConfiguration> kpiConfigurations = new ArrayList<>();
        kpiConfigurations.add(
                new KpiConfiguration(0L, SOME_KPI_NAME, true, ThresholdType.FIXED, SOME_KPI_VALUE_AS_DOUBLE, null, null, null));
        sessionConfiguration.setKpiConfigs(kpiConfigurations);
        return sessionConfiguration;
    }
}
