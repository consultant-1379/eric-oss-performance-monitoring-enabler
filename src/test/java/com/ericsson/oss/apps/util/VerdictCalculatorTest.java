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
package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.model.KpiTypeEnum.LTE;
import static com.ericsson.oss.apps.model.KpiTypeEnum.NR_NSA;
import static com.ericsson.oss.apps.model.KpiTypeEnum.NR_SA;
import static com.ericsson.oss.apps.model.VerdictEnum.DEGRADED;
import static com.ericsson.oss.apps.model.VerdictEnum.NOT_DEGRADED;
import static com.ericsson.oss.apps.model.VerdictEnum.NOT_POSSIBLE;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_LATENCY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_MAC_DRB_THROUGHPUT;
import static com.ericsson.oss.apps.util.Constants.AVG_DL_PDCP_UE_THROUGHPUT_CELL;
import static com.ericsson.oss.apps.util.Constants.AVG_UL_MAC_UE_THROUGHPUT;
import static com.ericsson.oss.apps.util.Constants.AVG_UL_PDCP_UE_THROUGHPUT_CELL;
import static com.ericsson.oss.apps.util.Constants.CELL_AVAILABILITY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.CELL_HANDOVER_SUCCESS_RATE_HOURLY;
import static com.ericsson.oss.apps.util.Constants.DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB;
import static com.ericsson.oss.apps.util.Constants.ENDC_PS_CELL_CHANGE_SUCCESS_RATE;
import static com.ericsson.oss.apps.util.Constants.ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB;
import static com.ericsson.oss.apps.util.Constants.E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY;
import static com.ericsson.oss.apps.util.Constants.INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_POSSIBLE_COUNT;
import static com.ericsson.oss.apps.util.Constants.NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB;
import static com.ericsson.oss.apps.util.Constants.UL_PUSCH_SINR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.VOIP_CELL_INTEGRITY_HOURLY;
import static com.ericsson.oss.apps.util.KpiConstants.KPIS;
import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.model.KpiConfiguration;
import com.ericsson.oss.apps.model.KpiTypeEnum;
import com.ericsson.oss.apps.model.KpiVerdict;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.ThresholdType;
import com.ericsson.oss.apps.model.ThresholdTypeEnum;
import com.ericsson.oss.apps.model.VerdictEnum;
import com.ericsson.oss.apps.model.VerdictMessage;
import com.ericsson.oss.apps.service.MetricService;

@SpringBootTest
@ActiveProfiles("test")
class VerdictCalculatorTest {
    private static final ZonedDateTime EXECUTION_TIME = ZonedDateTime.of(2023, 12, 5, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final Double FIXED_THRESHOLD = 85D;
    private static final String TDD_SUFFIX = "_tdd";
    private static final String FDD_SUFFIX = "_fdd";
    @MockBean
    private MetricService metricService;

    @Test
    void whenLteConfig_andUsingFixedThreshold_verifyKpiVerdictsAreCorrect() {
        final var config = sessionConfiguration(LTE);

        final var objectUnderTest = new VerdictCalculator(PME_SESSION_ID, EXECUTION_TIME, config,
                Map.of(FDN_FDD, kpiResult(Map.of(
                        CELL_AVAILABILITY_HOURLY.concat(FDD_SUFFIX), 75D,
                        CELL_HANDOVER_SUCCESS_RATE_HOURLY.concat(FDD_SUFFIX), 80D,
                        INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY.concat(FDD_SUFFIX), FIXED_THRESHOLD,
                        VOIP_CELL_INTEGRITY_HOURLY.concat(FDD_SUFFIX), 95D
                ))), metricService);
        final var verdictMessage = objectUnderTest.createVerdict(FDN_FDD);

        assertThat(verdictMessage)
                .isNotNull()
                .extracting(VerdictMessage::getFdn, VerdictMessage::getPmeSessionId, VerdictMessage::getTimestamp)
                .containsExactly(FDN_FDD, PME_SESSION_ID, EXECUTION_TIME.toInstant());

        assertThat(verdictMessage.getKpiVerdicts())
                .hasSize(10)
                .containsExactlyInAnyOrder(
                        fixedVerdict(AVG_DL_LATENCY_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_DL_PDCP_UE_THROUGHPUT_CELL, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_UL_PDCP_UE_THROUGHPUT_CELL, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(CELL_AVAILABILITY_HOURLY, LTE, DEGRADED, 75D),
                        fixedVerdict(CELL_HANDOVER_SUCCESS_RATE_HOURLY, LTE, DEGRADED, 80D),
                        fixedVerdict(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY, LTE, NOT_DEGRADED, FIXED_THRESHOLD),
                        fixedVerdict(UL_PUSCH_SINR_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(VOIP_CELL_INTEGRITY_HOURLY, LTE, NOT_DEGRADED, 95D));

        verifyMetrics(2, 2, 6);
    }

    @Test
    void whenLteTddConfig_andUsingFixedThreshold_verifyKpiVerdictsAreCorrect() {
        final var config = sessionConfiguration(LTE);

        final var objectUnderTest = new VerdictCalculator(PME_SESSION_ID, EXECUTION_TIME, config,
                Map.of(FDN_FDD, kpiResult(Map.of(
                        CELL_AVAILABILITY_HOURLY.concat(TDD_SUFFIX), 75D,
                        CELL_HANDOVER_SUCCESS_RATE_HOURLY.concat(TDD_SUFFIX), 80D,
                        INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY.concat(TDD_SUFFIX), FIXED_THRESHOLD,
                        VOIP_CELL_INTEGRITY_HOURLY.concat(TDD_SUFFIX), 95D
                ))), metricService);
        final var verdictMessage = objectUnderTest.createVerdict(FDN_FDD);

        assertThat(verdictMessage)
                .isNotNull()
                .extracting(VerdictMessage::getFdn, VerdictMessage::getPmeSessionId, VerdictMessage::getTimestamp)
                .containsExactly(FDN_FDD, PME_SESSION_ID, EXECUTION_TIME.toInstant());

        assertThat(verdictMessage.getKpiVerdicts())
                .hasSize(10)
                .containsExactlyInAnyOrder(
                        fixedVerdict(AVG_DL_LATENCY_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_DL_PDCP_UE_THROUGHPUT_CELL, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_UL_PDCP_UE_THROUGHPUT_CELL, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(CELL_AVAILABILITY_HOURLY, LTE, DEGRADED, 75D),
                        fixedVerdict(CELL_HANDOVER_SUCCESS_RATE_HOURLY, LTE, DEGRADED, 80D),
                        fixedVerdict(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY, LTE, NOT_DEGRADED, FIXED_THRESHOLD),
                        fixedVerdict(UL_PUSCH_SINR_HOURLY, LTE, NOT_POSSIBLE, null),
                        fixedVerdict(VOIP_CELL_INTEGRITY_HOURLY, LTE, NOT_DEGRADED, 95D));

        verifyMetrics(2, 2, 6);
    }

    @Test
    void whenNrNsaConfig_andUsingFixedThreshold_verifyKpiVerdictsAreCorrect() {
        final var config = sessionConfiguration(NR_NSA);

        final var objectUnderTest = new VerdictCalculator(PME_SESSION_ID, EXECUTION_TIME, config,
                Map.of(FDN_FDD, kpiResult(Map.of(
                        ENDC_PS_CELL_CHANGE_SUCCESS_RATE, 75D,
                        ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB, 80D,
                        SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB, FIXED_THRESHOLD,
                        PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY, 95D
                ))), metricService);
        final var verdictMessage = objectUnderTest.createVerdict(FDN_FDD);

        assertThat(verdictMessage)
                .isNotNull()
                .extracting(VerdictMessage::getFdn, VerdictMessage::getPmeSessionId, VerdictMessage::getTimestamp)
                .containsExactly(FDN_FDD, PME_SESSION_ID, EXECUTION_TIME.toInstant());

        assertThat(verdictMessage.getKpiVerdicts())
                .hasSize(7)
                .containsExactlyInAnyOrder(
                        fixedVerdict(NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY, NR_NSA, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_DL_MAC_DRB_THROUGHPUT, NR_NSA, NOT_POSSIBLE, null),
                        fixedVerdict(AVG_UL_MAC_UE_THROUGHPUT, NR_NSA, NOT_POSSIBLE, null),
                        fixedVerdict(ENDC_PS_CELL_CHANGE_SUCCESS_RATE, NR_NSA, DEGRADED, 75D),
                        fixedVerdict(ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB, NR_NSA, DEGRADED, 80D),
                        fixedVerdict(PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY, NR_NSA, NOT_DEGRADED, 95D),
                        fixedVerdict(SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB, NR_NSA, NOT_DEGRADED, FIXED_THRESHOLD));

        verifyMetrics(2, 2, 3);
    }

    @Test
    void whenNrSaConfig_andUsingFixedThreshold_verifyKpiVerdictsAreCorrect() {
        final var config = sessionConfiguration(NR_SA);

        final var objectUnderTest = new VerdictCalculator(PME_SESSION_ID, EXECUTION_TIME, config,
                Map.of(FDN_FDD, kpiResult(Map.of(
                        NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY, 75D,
                        NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY, 95D
                ))), metricService);
        final var verdictMessage = objectUnderTest.createVerdict(FDN_FDD);

        assertThat(verdictMessage)
                .isNotNull()
                .extracting(VerdictMessage::getFdn, VerdictMessage::getPmeSessionId, VerdictMessage::getTimestamp)
                .containsExactly(FDN_FDD, PME_SESSION_ID, EXECUTION_TIME.toInstant());

        assertThat(verdictMessage.getKpiVerdicts())
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        fixedVerdict(NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY, NR_SA, DEGRADED, 75D),
                        fixedVerdict(NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY, NR_SA, NOT_DEGRADED, 95D));

        verifyMetrics(1, 1, 0);
    }

    private KpiResult kpiResult(final Map<String, Double> kpiValues) {
        final var kpiResult = new KpiResult();
        kpiResult.putAll(kpiValues);
        return kpiResult;
    }

    private KpiVerdict fixedVerdict(final String name, final KpiTypeEnum type, final VerdictEnum verdict, final Double value) {
        return KpiVerdict.newBuilder()
                .setKpiName(name)
                .setKpiValue(value)
                .setThresholdValue(FIXED_THRESHOLD)
                .setVerdict(verdict)
                .setThresholdType(ThresholdTypeEnum.FIXED)
                .setKpiType(type)
                .build();
    }

    private SessionConfiguration sessionConfiguration(final KpiTypeEnum kpiType) {
        final var kpiConfigs = KPIS.entrySet().stream()
                .filter(entry -> kpiType.equals(entry.getValue().getType()))
                .map(entry -> fixedConfig(entry.getKey()))
                .toList();

        final var sessionConfiguration = new SessionConfiguration();
        sessionConfiguration.setKpiConfigs(kpiConfigs);
        return sessionConfiguration;
    }

    private KpiConfiguration fixedConfig(final String name) {
        final var kpiConfig = new KpiConfiguration();

        kpiConfig.setKpiName(name);
        kpiConfig.setMonitor(true);
        kpiConfig.setThresholdType(ThresholdType.FIXED);
        kpiConfig.setFixedThresholdValue(FIXED_THRESHOLD);
        return kpiConfig;
    }

    private void verifyMetrics(final int degraded, final int notDegraded, final int notPossible) {
        verify(metricService, times(1)).increment(PME_VERDICT_DEGRADED_COUNT, degraded);
        verify(metricService, times(1)).increment(PME_VERDICT_NOT_DEGRADED_COUNT, notDegraded);
        verify(metricService, times(1)).increment(PME_VERDICT_NOT_POSSIBLE_COUNT, notPossible);
    }
}
