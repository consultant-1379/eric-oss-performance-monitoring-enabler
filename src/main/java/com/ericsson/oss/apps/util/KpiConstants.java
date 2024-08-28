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
import static com.ericsson.oss.apps.util.Constants.NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB;
import static com.ericsson.oss.apps.util.Constants.UL_PUSCH_SINR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.VOIP_CELL_INTEGRITY_HOURLY;
import static java.util.Map.entry;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.model.KpiTypeEnum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KpiConstants {
    public static final Map<String, Kpi> KPIS = Map.ofEntries(
            entry(AVG_DL_LATENCY_HOURLY, new Kpi(LTE, lte(AVG_DL_LATENCY_HOURLY))),
            entry(AVG_DL_PDCP_UE_THROUGHPUT_CELL, new Kpi(LTE, lte(AVG_DL_PDCP_UE_THROUGHPUT_CELL))),
            entry(AVG_UL_PDCP_UE_THROUGHPUT_CELL, new Kpi(LTE, lte(AVG_UL_PDCP_UE_THROUGHPUT_CELL))),
            entry(CELL_AVAILABILITY_HOURLY, new Kpi(LTE, lte(CELL_AVAILABILITY_HOURLY))),
            entry(CELL_HANDOVER_SUCCESS_RATE_HOURLY, new Kpi(LTE, lte(CELL_HANDOVER_SUCCESS_RATE_HOURLY))),
            entry(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB, new Kpi(LTE, lte(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB))),
            entry(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY, new Kpi(LTE, lte(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY))),
            entry(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY, new Kpi(LTE, lte(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY))),
            entry(UL_PUSCH_SINR_HOURLY, new Kpi(LTE, lte(UL_PUSCH_SINR_HOURLY))),
            entry(VOIP_CELL_INTEGRITY_HOURLY, new Kpi(LTE, lte(VOIP_CELL_INTEGRITY_HOURLY))),

            entry(NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY, new Kpi(NR_NSA, NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY)),
            entry(AVG_DL_MAC_DRB_THROUGHPUT, new Kpi(NR_NSA, AVG_DL_MAC_DRB_THROUGHPUT)),
            entry(AVG_UL_MAC_UE_THROUGHPUT, new Kpi(NR_NSA, AVG_UL_MAC_UE_THROUGHPUT)),
            entry(ENDC_PS_CELL_CHANGE_SUCCESS_RATE, new Kpi(NR_NSA, ENDC_PS_CELL_CHANGE_SUCCESS_RATE)),
            entry(ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB, new Kpi(NR_NSA, ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB)),
            entry(PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY, new Kpi(NR_NSA, PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY)),
            entry(SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB, new Kpi(NR_NSA, SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB)),

            entry(NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY, new Kpi(NR_SA, NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY)),
            entry(NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY, new Kpi(NR_SA, NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY)));

    private static final String TDD_SUFFIX = "_tdd";
    private static final String FDD_SUFFIX = "_fdd";

    private static List<String> lte(final String kpiName) {
        return List.of(kpiName.concat(FDD_SUFFIX), kpiName.concat(TDD_SUFFIX));
    }

    @Getter
    @RequiredArgsConstructor
    public static class Kpi {
        private final KpiTypeEnum type;
        private final List<String> kpiNames;

        public Kpi(final KpiTypeEnum type, final String... kpiNames) {
            this(type, List.of(kpiNames));
        }
    }
}
