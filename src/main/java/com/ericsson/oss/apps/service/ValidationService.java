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

import static com.ericsson.oss.apps.util.Constants.AVG_DL_LATENCY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_MAC_DRB_THROUGHPUT;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_PDCP_UE_THROUGHPUT_CELL;
import static com.ericsson.oss.apps.util.Constants.AVG_UL_MAC_UE_THROUGHPUT;
import static com.ericsson.oss.apps.util.Constants.AVG_UL_PDCP_UE_THROUGHPUT_CELL;
import static com.ericsson.oss.apps.util.Constants.CELL_AVAILABILITY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.CELL_HANDOVER_SUCCESS_RATE_HOURLY;
import static com.ericsson.oss.apps.util.Constants.CLIENT_ID_LENGTH;
import static com.ericsson.oss.apps.util.Constants.CLIENT_ID_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.CLIENT_ID_PATTERN;
import static com.ericsson.oss.apps.util.Constants.CONFIG_NAME_INVALID_LENGTH;
import static com.ericsson.oss.apps.util.Constants.CONFIG_NAME_INVALID_PATTERN;
import static com.ericsson.oss.apps.util.Constants.CONFIG_NAME_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB;
import static com.ericsson.oss.apps.util.Constants.ENDC_PS_CELL_CHANGE_SUCCESS_RATE;
import static com.ericsson.oss.apps.util.Constants.ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB;
import static com.ericsson.oss.apps.util.Constants.E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY;
import static com.ericsson.oss.apps.util.Constants.FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY;
import static com.ericsson.oss.apps.util.Constants.FIXED_THRESHOLD_VALUE_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.KPI_NAME_INVALID;
import static com.ericsson.oss.apps.util.Constants.KPI_VALUE_INVALID_STEP_SIZE;
import static com.ericsson.oss.apps.util.Constants.KPI_VALUE_NOT_IN_RANGE;
import static com.ericsson.oss.apps.util.Constants.MAX_KPIS_PER_CONFIGURATIONS_EXCEEDED;
import static com.ericsson.oss.apps.util.Constants.NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB;
import static com.ericsson.oss.apps.util.Constants.SESSION_CONFIG_ID_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.SESSION_DURATION_RANGE;
import static com.ericsson.oss.apps.util.Constants.SESSION_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.SESSION_REFERENCE_LENGTH;
import static com.ericsson.oss.apps.util.Constants.SESSION_REFERENCE_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.SESSION_REFERENCE_PATTERN;
import static com.ericsson.oss.apps.util.Constants.SESSION_STATUS_INVALID;
import static com.ericsson.oss.apps.util.Constants.UL_PUSCH_SINR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.VOIP_CELL_INTEGRITY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.WEEKEND_DAYS_NON_NULL;
import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeConfigurationUpdate;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.api.model.EpmeSessionStopRequest;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.config.PersistenceConfig;
import com.ericsson.oss.apps.exception.ControllerDetailException;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ValidationService {

    protected static final KpiValidationData PERCENT_VALIDATION = new KpiValidationData(0.00D, 100.00D, 0.01D);
    protected static final KpiValidationData KBPS_1GB_VALIDATION = new KpiValidationData(0.00D, 1000000.00D, 0.01D);
    protected static final KpiValidationData KBPS_100MB_VALIDATION = new KpiValidationData(0.00D, 100000.00D, 0.01D);
    protected static final KpiValidationData MS_VALIDATION = new KpiValidationData(0.00D, 1000.00D, 0.1D);
    protected static final Map<String, KpiValidationData> KPI_VALIDATION_DATA = Map.ofEntries(
            entry(ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB, PERCENT_VALIDATION),
            entry(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB, PERCENT_VALIDATION),
            entry(SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB, PERCENT_VALIDATION),
            entry(AVG_DL_MAC_DRB_THROUGHPUT, KBPS_1GB_VALIDATION),
            entry(AVG_UL_MAC_UE_THROUGHPUT, KBPS_1GB_VALIDATION),
            entry(NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY, KBPS_1GB_VALIDATION),
            entry(ENDC_PS_CELL_CHANGE_SUCCESS_RATE, PERCENT_VALIDATION),
            entry(NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY, PERCENT_VALIDATION),
            entry(NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY, PERCENT_VALIDATION),
            entry(PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY, PERCENT_VALIDATION),
            entry(CELL_AVAILABILITY_HOURLY, PERCENT_VALIDATION),
            entry(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY, PERCENT_VALIDATION),
            entry(AVG_UL_PDCP_UE_THROUGHPUT_CELL, KBPS_100MB_VALIDATION),
            entry(AVG_DL_PDCP_UE_THROUGHPUT_CELL, KBPS_100MB_VALIDATION),
            entry(UL_PUSCH_SINR_HOURLY, PERCENT_VALIDATION),
            entry(CELL_HANDOVER_SUCCESS_RATE_HOURLY, PERCENT_VALIDATION),
            entry(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY, PERCENT_VALIDATION),
            entry(AVG_DL_LATENCY_HOURLY, MS_VALIDATION),
            entry(VOIP_CELL_INTEGRITY_HOURLY, PERCENT_VALIDATION));
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w-_]+$");
    private static final int CONFIG_NAME_MAX_LEN = 255;
    private static final int CONFIG_NAME_MIN_LEN = 1;
    private static final int MIN_AVG_CONNECTED_USERS_RELIABILITY_MAX = 100;
    private static final int MIN_AVG_CONNECTED_USERS_RELIABILITY_MIN = 1;
    private static final int SIX_DECIMAL_PLACES = 1000000;

    private final PersistenceConfig persistenceConfig;

    public void validateClientId(final String clientId) {
        if (Objects.isNull(clientId)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CLIENT_ID_NON_NULL);
        }
        if (!NAME_PATTERN.matcher(clientId).matches()) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CLIENT_ID_PATTERN);
        }
        if (clientId.length() < 4 || clientId.length() > 64) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CLIENT_ID_LENGTH);
        }
    }

    public void validateSessionReference(final EpmeSessionRequest sessionRequest) {
        if (Objects.isNull(sessionRequest)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_NON_NULL);
        }

        final var sessionReference = sessionRequest.getSessionReference();
        if (Objects.isNull(sessionReference)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_REFERENCE_NON_NULL);
        }
        if (!NAME_PATTERN.matcher(sessionReference).matches()) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_REFERENCE_PATTERN);
        }
        if (sessionReference.length() < 4 || sessionReference.length() > 64) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_REFERENCE_LENGTH);
        }
    }

    public void validateSessionRequest(final EpmeSessionRequest sessionRequest) {
        if (Objects.isNull(sessionRequest)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_NON_NULL);
        }

        final var duration = sessionRequest.getDuration();
        if (Objects.nonNull(duration) && (duration < 1 || duration > 24)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_DURATION_RANGE);
        }

        final var pmeConfigId = sessionRequest.getPmeConfigId();
        if (Objects.isNull(pmeConfigId)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_CONFIG_ID_NON_NULL);
        }
    }

    public void validateSessionStopRequest(final EpmeSessionStopRequest sessionRequest) {
        if (Objects.isNull(sessionRequest)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_NON_NULL);
        }

        final var status = sessionRequest.getStatus();
        if (!EpmeSessionStopRequest.StatusEnum.STOPPED.equals(status)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, SESSION_STATUS_INVALID);
        }
    }

    public void validateConfigurationRequest(final EpmeConfigurationRequest epmeConfigurationRequest) {
        validateConfigurationName(epmeConfigurationRequest.getName());
        validateWeekendDays(epmeConfigurationRequest.getWeekendDays());
        validateFixedThresholdKpis(epmeConfigurationRequest.getFixedThresholdKpis());
    }

    public void validateConfigurationUpdate(final EpmeConfigurationUpdate epmeConfigurationUpdate) {
        validateWeekendDays(epmeConfigurationUpdate.getWeekendDays());
        validateFixedThresholdKpis(epmeConfigurationUpdate.getFixedThresholdKpis());
    }

    protected void validateConfigurationName(final String name) {
        if (Objects.isNull(name)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CONFIG_NAME_NON_NULL);
        }
        if (name.length() < CONFIG_NAME_MIN_LEN || name.length() > CONFIG_NAME_MAX_LEN) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CONFIG_NAME_INVALID_LENGTH);
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, CONFIG_NAME_INVALID_PATTERN);
        }
    }

    protected void validateWeekendDays(final EpmeWeekendDays epmeWeekendDays) {
        if (Objects.isNull(epmeWeekendDays)) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, WEEKEND_DAYS_NON_NULL);
        }
    }

    protected void validateFixedThresholdKpis(final Set<EpmeFixed> fixedThresholdKpis) {
        if (Objects.isNull(fixedThresholdKpis) || fixedThresholdKpis.isEmpty()) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY);
        }

        if (fixedThresholdKpis.size() > persistenceConfig.getMaxKpisPerConfigurations()) {
            throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED,
                    MAX_KPIS_PER_CONFIGURATIONS_EXCEEDED + persistenceConfig.getMaxKpisPerConfigurations());
        }

        validateKpiEntries(fixedThresholdKpis);
    }

    private void validateKpiEntries(final Set<EpmeFixed> fixedThresholdKpis) {
        for (final EpmeFixed epmeFixed : fixedThresholdKpis) {
            if (KPI_VALIDATION_DATA.containsKey(epmeFixed.getKpiName())) {
                if (Objects.isNull(epmeFixed.getFixedThreshold())) {
                    throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED, FIXED_THRESHOLD_VALUE_NON_NULL);
                }
                final KpiValidationData kpiValidationData = KPI_VALIDATION_DATA.get(epmeFixed.getKpiName());
                if (kpiValidationData.isInRange(epmeFixed.getFixedThreshold())) {
                    if (!thresholdWithinStepSize(epmeFixed.getFixedThreshold(), kpiValidationData.getStepSize())) {
                        throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED,
                                String.format(KPI_VALUE_INVALID_STEP_SIZE, epmeFixed.getKpiName(), kpiValidationData.getStepSize()));
                    }
                } else {
                    throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED,
                            String.format(KPI_VALUE_NOT_IN_RANGE, epmeFixed.getKpiName(), kpiValidationData.getRange()));
                }
            } else {
                throw new ControllerDetailException(BAD_REQUEST, VALIDATION_FAILED,
                        String.format(KPI_NAME_INVALID, epmeFixed.getKpiName(), KPI_VALIDATION_DATA.keySet()));
            }
        }
    }

    private boolean thresholdWithinStepSize(final Double thresholdValue, final Double stepSize) {
        final Double kpiValueRnd = truncateTo(((int) (thresholdValue / stepSize)) * stepSize);
        return kpiValueRnd.equals(thresholdValue);
    }

    private Double truncateTo(final Double value) {
        return Math.floor(value * SIX_DECIMAL_PLACES) / SIX_DECIMAL_PLACES;
    }

    @AllArgsConstructor
    protected static class KpiValidationData {
        private final Double min;
        private final Double max;

        private final double stepSize;

        public boolean isInRange(final Double value) {
            return (value >= min && value <= max);
        }

        public String getRange() {
            return "[" + min + ".." + max + "]";
        }

        public double getStepSize() {
            return stepSize;
        }
    }

}
