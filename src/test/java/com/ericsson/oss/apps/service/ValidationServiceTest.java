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

import static com.ericsson.oss.apps.service.ValidationService.KPI_VALIDATION_DATA;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_LATENCY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY;
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
import static com.ericsson.oss.apps.util.Constants.ENDC_PS_CELL_CHANGE_SUCCESS_RATE;
import static com.ericsson.oss.apps.util.Constants.ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB;
import static com.ericsson.oss.apps.util.Constants.DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB;
import static com.ericsson.oss.apps.util.Constants.E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY;
import static com.ericsson.oss.apps.util.Constants.FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY;
import static com.ericsson.oss.apps.util.Constants.FIXED_THRESHOLD_VALUE_NON_NULL;
import static com.ericsson.oss.apps.util.Constants.INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.KPI_NAME_INVALID;
import static com.ericsson.oss.apps.util.Constants.KPI_VALUE_INVALID_STEP_SIZE;
import static com.ericsson.oss.apps.util.Constants.KPI_VALUE_NOT_IN_RANGE;
import static com.ericsson.oss.apps.util.Constants.MAX_KPIS_PER_CONFIGURATIONS_EXCEEDED;
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
import static com.ericsson.oss.apps.util.Constants.UL_PUSCH_SINR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.VOIP_CELL_INTEGRITY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.WEEKEND_DAYS_NON_NULL;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.DETAIL_FIELD;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.REASON_FIELD;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static com.ericsson.oss.apps.util.TestConstants.STATUS_FIELD;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeConfigurationUpdate;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.api.model.EpmeSessionStopRequest;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.config.PersistenceConfig;
import com.ericsson.oss.apps.exception.ControllerDetailException;

class ValidationServiceTest {

    private static final String VALID_KPI_NAME = "en_dc_setup_sr_captured_gnodeb_hourly";
    private final PersistenceConfig persistenceConfig = new PersistenceConfig();
    private final ValidationService objectUnderTest = new ValidationService(persistenceConfig);

    private static Stream<Arguments> clientIdArgs() {
        return Stream.of(
                Arguments.of(null, CLIENT_ID_NON_NULL),
                Arguments.of("string with spaces", CLIENT_ID_PATTERN),
                Arguments.of("X".repeat(65), CLIENT_ID_LENGTH),
                Arguments.of("X", CLIENT_ID_LENGTH));
    }

    private static Stream<Arguments> sessionRequestArgs() {
        return Stream.of(
                Arguments.of(null, SESSION_NON_NULL),
                Arguments.of(validSession().duration(0), SESSION_DURATION_RANGE),
                Arguments.of(validSession().duration(25), SESSION_DURATION_RANGE),
                Arguments.of(validSession().pmeConfigId(null), SESSION_CONFIG_ID_NON_NULL));
    }

    private static Stream<Arguments> sessionReferenceArgs() {
        return Stream.of(
                Arguments.of(null, SESSION_NON_NULL),
                Arguments.of(validSession().sessionReference(null), SESSION_REFERENCE_NON_NULL),
                Arguments.of(validSession().sessionReference("session with spaces"), SESSION_REFERENCE_PATTERN),
                Arguments.of(validSession().sessionReference("X".repeat(65)), SESSION_REFERENCE_LENGTH),
                Arguments.of(validSession().sessionReference("X"), SESSION_REFERENCE_LENGTH));
    }

    private static Stream<Arguments> configurationNameArgs() {
        return Stream.of(
                Arguments.of(null, CONFIG_NAME_NON_NULL),
                Arguments.of("string with spaces", CONFIG_NAME_INVALID_PATTERN),
                Arguments.of("X".repeat(256), CONFIG_NAME_INVALID_LENGTH),
                Arguments.of("", CONFIG_NAME_INVALID_LENGTH));
    }

    private static Stream<Arguments> weekendDaysArgs() {
        return Stream.of(
                Arguments.of(null, WEEKEND_DAYS_NON_NULL));
    }

    private static Stream<Arguments> fixedThresholdKpisArgs() {
        return Stream.of(
                Arguments.of(null, FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY),
                Arguments.of(Collections.emptySet(), FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY));
    }

    private static Stream<Arguments> fixedThresholdPercentKpiArgs() {
        return Stream.of(
                Arguments.of(ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB),
                Arguments.of(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB),
                Arguments.of(SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB),
                Arguments.of(ENDC_PS_CELL_CHANGE_SUCCESS_RATE),
                Arguments.of(NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY),
                Arguments.of(NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY),
                Arguments.of(PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY),
                Arguments.of(CELL_AVAILABILITY_HOURLY),
                Arguments.of(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY),
                Arguments.of(UL_PUSCH_SINR_HOURLY),
                Arguments.of(CELL_HANDOVER_SUCCESS_RATE_HOURLY),
                Arguments.of(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY),
                Arguments.of(VOIP_CELL_INTEGRITY_HOURLY));
    }

    private static Stream<Arguments> fixedThreshold1GbKpiArgs() {
        return Stream.of(
                Arguments.of(AVG_DL_MAC_DRB_THROUGHPUT),
                Arguments.of(AVG_UL_MAC_UE_THROUGHPUT),
                Arguments.of(NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY));
    }

    private static Stream<Arguments> fixedThreshold100MbKpiArgs() {
        return Stream.of(
                Arguments.of(AVG_UL_PDCP_UE_THROUGHPUT_CELL),
                Arguments.of(AVG_DL_PDCP_UE_THROUGHPUT_CELL));
    }

    private static Stream<Arguments> fixedThresholdMilliSecKpiArgs() {
        return Stream.of(
                Arguments.of(AVG_DL_LATENCY_HOURLY),
                Arguments.of(AVG_DL_LATENCY_HOURLY));
    }

    private static Stream<Arguments> fixedThresholdOneHundredthStepSizeKpiArgs() {
        return Stream.of(fixedThresholdPercentKpiArgs(), Stream.concat(fixedThreshold1GbKpiArgs(), fixedThreshold100MbKpiArgs())).flatMap(i -> i);
    }

    private static Stream<Arguments> fixedThresholdOneTenthStepSizeKpiArgs() {
        return fixedThresholdMilliSecKpiArgs();
    }

    private static Stream<Arguments> fixedThresholdKpisWithValidRangeAndStepsArgs() {
        return Stream.of(
                // percent kpis
                getArgsWithKpiValue(fixedThresholdPercentKpiArgs(), 99.99D),
                getArgsWithKpiValue(fixedThresholdPercentKpiArgs(), 0.01D),
                getArgsWithKpiValue(fixedThresholdPercentKpiArgs(), 0D),
                getArgsWithKpiValue(fixedThresholdPercentKpiArgs(), 100D),
                // 1Gb kpis
                getArgsWithKpiValue(fixedThreshold1GbKpiArgs(), 999999.99D),
                getArgsWithKpiValue(fixedThreshold1GbKpiArgs(), 0.01D),
                getArgsWithKpiValue(fixedThreshold1GbKpiArgs(), 0D),
                getArgsWithKpiValue(fixedThreshold1GbKpiArgs(), 1000000D),
                // 100Mb kpis
                getArgsWithKpiValue(fixedThreshold100MbKpiArgs(), 99999.9D),
                getArgsWithKpiValue(fixedThreshold100MbKpiArgs(), 0.01D),
                getArgsWithKpiValue(fixedThreshold100MbKpiArgs(), 0D),
                getArgsWithKpiValue(fixedThreshold100MbKpiArgs(), 100000D),
                // MilliSec kpis
                getArgsWithKpiValue(fixedThresholdMilliSecKpiArgs(), 999.9D),
                getArgsWithKpiValue(fixedThresholdMilliSecKpiArgs(), 0.1D),
                getArgsWithKpiValue(fixedThresholdMilliSecKpiArgs(), 0D),
                getArgsWithKpiValue(fixedThresholdMilliSecKpiArgs(), 1000D))
                .flatMap(i -> i);
    }

    private static Stream<Arguments> getArgsWithKpiValue(final Stream<Arguments> args, final Double value) {
        return args.map(k -> Arguments.of(Arrays.stream(k.get()).toArray()[0], value));
    }

    private static EpmeSessionRequest validSession() {
        return new EpmeSessionRequest(SESSION_REFERENCE, Long.toString(PME_CONFIGURATION_ID));
    }

    private static EpmeSessionStopRequest sessionStopRequest() {
        return new EpmeSessionStopRequest(EpmeSessionStopRequest.StatusEnum.STOPPED);
    }

    private static String getInvalidRangeMsg(final String kpiName) {
        return String.format(KPI_VALUE_NOT_IN_RANGE, kpiName, KPI_VALIDATION_DATA.get(kpiName).getRange());
    }

    private static String getInvalidStepMsg(final String kpiName) {
        return String.format(KPI_VALUE_INVALID_STEP_SIZE, kpiName, KPI_VALIDATION_DATA.get(kpiName).getStepSize());
    }

    @BeforeEach
    void setUp() {
        persistenceConfig.setMaxKpisPerConfigurations(1000);
    }

    @Test
    void whenValidateClientId_andClientIdIsValid_verifyNoExceptionThrown() {
        assertThatCode(() -> objectUnderTest.validateClientId(CLIENT_APP_ID))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("clientIdArgs")
    void whenValidateClientId_andClientIdIsInvalid_verifyExceptionThrown(final String clientId, final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateClientId(clientId))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @Test
    void whenValidateSessionRequest_andSessionIsValid_verifyNoExceptionThrown() {
        assertThatCode(() -> objectUnderTest.validateSessionRequest(validSession()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("sessionRequestArgs")
    void whenValidateSessionRequest_andSessionIsInvalid_verifyExceptionThrown(final EpmeSessionRequest sessionRequest, final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateSessionRequest(sessionRequest))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @Test
    void whenValidateSessionStopRequest_andSessionIsValid_verifyNoExceptionThrown() {
        assertThatCode(() -> objectUnderTest.validateSessionStopRequest(sessionStopRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void whenValidateSessionStopRequest_andSessionIsInvalid_verifyExceptionThrown() {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateSessionStopRequest(null))
                .extracting(STATUS_FIELD, REASON_FIELD, DETAIL_FIELD)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, SESSION_NON_NULL);
    }

    @ParameterizedTest
    @MethodSource("sessionReferenceArgs")
    void whenValidateSessionReference_andSessionReferenceIsInvalid_verifyExceptionThrown(final EpmeSessionRequest sessionRequest,
            final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateSessionReference(sessionRequest))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @Test
    void whenValidateConfigurationRequestForValidConfiguration_thenNoExceptionThrown() {
        final Set<EpmeFixed> epmeFixed = new HashSet<>();
        epmeFixed.add(new EpmeFixed(VALID_KPI_NAME, 98.9D));
        final EpmeConfigurationRequest epmeConfigurationRequest = new EpmeConfigurationRequest(
                "configuration", EpmeWeekendDays.SATURDAY_SUNDAY,
                epmeFixed);
        assertThatCode(() -> objectUnderTest.validateConfigurationRequest(epmeConfigurationRequest))
                .doesNotThrowAnyException();
    }

    @Test
    void whenValidateConfigurationUpdateForValidConfiguration_thenNoExceptionThrown() {
        final Set<EpmeFixed> epmeFixed = new HashSet<>();
        epmeFixed.add(new EpmeFixed(VALID_KPI_NAME, 98.9D));
        final EpmeConfigurationUpdate epmeConfigurationUpdate = new EpmeConfigurationUpdate(
                EpmeWeekendDays.SATURDAY_SUNDAY,
                epmeFixed);
        assertThatCode(() -> objectUnderTest.validateConfigurationUpdate(epmeConfigurationUpdate))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("configurationNameArgs")
    void whenConfigurationNameIsInvalid_thenExceptionThrown(final String configurationName, final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateConfigurationName(configurationName))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @ParameterizedTest
    @MethodSource("weekendDaysArgs")
    void whenWeekendDaysIsInvalid_thenExceptionThrown(final EpmeWeekendDays weekendDays, final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateWeekendDays(weekendDays))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @Test
    void whenKpiNameIsInvalid_thenExceptionThrown() {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed("an_invalid_kpi", 98.9D));

        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                        String.format(KPI_NAME_INVALID, "an_invalid_kpi", KPI_VALIDATION_DATA.keySet()));
    }

    @Test
    void whenNumberOfKpisExceedsTheMaxAllowed_thenExceptionThrown() {
        persistenceConfig.setMaxKpisPerConfigurations(0);
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(VALID_KPI_NAME, 98.9D));

        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                        MAX_KPIS_PER_CONFIGURATIONS_EXCEEDED + persistenceConfig.getMaxKpisPerConfigurations());
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdKpisArgs")
    void whenFixedThresholdKpisIsInvalid_thenExceptionThrown(final Set<EpmeFixed> fixedThresholdKpis, final String detail) {
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, detail);
    }

    @Test
    void whenFixedThresholdKpisHasValidKpiNameButNullFixedThresholdValue_thenExceptionThrown() {
        final Set<EpmeFixed> epmeFixed = new HashSet<>();
        epmeFixed.add(new EpmeFixed(VALID_KPI_NAME, null));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(epmeFixed))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, FIXED_THRESHOLD_VALUE_NON_NULL);
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdPercentKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMinPercentRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, -0.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdPercentKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMaxPercentRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 100.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThreshold1GbKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMin1GbRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, -0.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThreshold1GbKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMax1GbRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 1000000.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThreshold100MbKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMin100MbRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, -0.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThreshold100MbKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMax100MbRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 100000.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdMilliSecKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMinMilliSecRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, -0.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdMilliSecKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidMaxMilliSecRange_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 1000.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidRangeMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdOneHundredthStepSizeKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidOneHundredthStepSize_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 0.091D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidStepMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdOneTenthStepSizeKpiArgs")
    void whenFixedThresholdKpisHasKpiWithInvalidOneTenthStepSize_thenExceptionThrown(final String kpiName) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, 0.01D));
        assertThatExceptionOfType(ControllerDetailException.class)
                .isThrownBy(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .extracting(ControllerDetailException::getStatus, ControllerDetailException::getReason, ControllerDetailException::getDetail)
                .containsExactly(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, getInvalidStepMsg(kpiName));
    }

    @ParameterizedTest
    @MethodSource("fixedThresholdKpisWithValidRangeAndStepsArgs")
    void whenFixedThresholdKpisHasValidKpiRange_thenNoExceptionThrown(final String kpiName, final Double value) {
        final Set<EpmeFixed> fixedThresholdKpis = new HashSet<>();
        fixedThresholdKpis.add(new EpmeFixed(kpiName, value));
        assertThatCode(() -> objectUnderTest.validateFixedThresholdKpis(fixedThresholdKpis))
                .doesNotThrowAnyException();
    }
}
