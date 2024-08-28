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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.api.model.EpmeConfiguration;
import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.model.KpiConfiguration;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.ThresholdType;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private final SessionConfigurationRepository sessionConfigurationRepository;

    public void deleteSessionConfigurationById(final long id) {
        try {
            final Optional<SessionConfiguration> optionalConfiguration = sessionConfigurationRepository.findById(id);

            if (optionalConfiguration.isPresent()) {
                sessionConfigurationRepository.deleteById(id);
            } else {
                log.error("Tried to delete Configuration that does not exist.");
                throw new IllegalArgumentException();
            }
        } catch (final DataAccessException e) {
            log.error("Failed to delete configuration with ID '{}' from the database.", id, e);
            throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, DELETE_FAILED); //NOPMD
        }
    }

    public SessionConfiguration createSessionConfiguration(final EpmeConfigurationRequest epmeConfigurationRequest) {
        final SessionConfiguration sessionConfig = new SessionConfiguration(epmeConfigurationRequest.getName(),
                epmeConfigurationRequest.getWeekendDays().toString());
        final List<KpiConfiguration> kpiConfigs = epmeConfigurationRequest.getFixedThresholdKpis().stream()
                .map(this::createKpiConfiguration)
                .collect(Collectors.toList());
        sessionConfig.setKpiConfigs(kpiConfigs);

        log.debug("Saving session configuration with name: {} to the database.", epmeConfigurationRequest.getName());
        return sessionConfigurationRepository.save(sessionConfig);
    }

    public SessionConfiguration updateSessionConfigurationById(final String configurationId, final SessionConfiguration newConfig) {
        try {
            final long id = Long.parseLong(configurationId);
            final Optional<SessionConfiguration> optionalConfiguration = sessionConfigurationRepository.findById(id);

            if (optionalConfiguration.isPresent()) {
                return updateExistingConfig(newConfig, optionalConfiguration.get());
            } else {
                log.error("Tried to update a Configuration that does not exist.");
                throw new IllegalArgumentException();
            }
        } catch (final DataAccessException e) {
            log.error("Failed to update configuration with ID '{}'.", configurationId, e);
            throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, UPDATE_FAILED); //NOPMD
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

    public boolean exists(final String name) {
        return sessionConfigurationRepository.existsByName(name);
    }

    public long count() {
        return sessionConfigurationRepository.count();
    }

    public EpmeConfiguration dto(final SessionConfiguration sessionConfiguration) {
        final Set<EpmeFixed> epmeFixedKpi = sessionConfiguration.getKpiConfigs()
                .stream()
                .map(kpiConfiguration -> new EpmeFixed(kpiConfiguration.getKpiName(),
                        Double.valueOf(DF.format(kpiConfiguration.getFixedThresholdValue()))))
                .collect(Collectors.toSet());

        return new EpmeConfiguration(
                Long.toString(sessionConfiguration.getId()),
                sessionConfiguration.getName(),
                EpmeWeekendDays.fromValue(sessionConfiguration.getWeekendDays().toUpperCase(Locale.ROOT)),
                epmeFixedKpi);
    }

    private SessionConfiguration updateExistingConfig(final SessionConfiguration newConfig, final SessionConfiguration existingConfiguration) {
        existingConfiguration.setNumberOfConnectedUsersForReliability(newConfig.getNumberOfConnectedUsersForReliability());
        existingConfiguration.setWeekendDays(newConfig.getWeekendDays());
        existingConfiguration.setKpiConfigs(newConfig.getKpiConfigs());
        return sessionConfigurationRepository.save(existingConfiguration);
    }

    public List<SessionConfiguration> findAll() {
        try {
            return sessionConfigurationRepository.findAll();
        } catch (final DataAccessException e) {
            log.error("Failed to get configurations from the database.", e);
            throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, GET_FAILED); //NOPMD
        }
    }

    public SessionConfiguration getConfig(final String configurationId) {
        try {
            final long id = Long.parseLong(configurationId);
            return sessionConfigurationRepository.findById(id).orElse(null);
        } catch (final NumberFormatException e) {
            log.error(CONFIGURATION_DOES_NOT_EXIST, e);
            throw new ControllerDetailException(HttpStatus.NOT_FOUND, PROVIDE_A_VALID_CONFIGURATION_ID, CONFIGURATION_DOES_NOT_EXIST); //NOPMD
        } catch (final DataAccessException e) {
            log.error("Failed to get configuration with ID '{}' from the database.", configurationId, e);
            throw new ControllerDetailException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, GET_FAILED); //NOPMD
        }
    }
}
