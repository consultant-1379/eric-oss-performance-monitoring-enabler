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

import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_POSSIBLE_COUNT;
import static com.ericsson.oss.apps.util.KpiConstants.KPIS;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.model.KpiConfiguration;
import com.ericsson.oss.apps.model.KpiVerdict;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.ThresholdTypeEnum;
import com.ericsson.oss.apps.model.VerdictEnum;
import com.ericsson.oss.apps.model.VerdictMessage;
import com.ericsson.oss.apps.service.MetricService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VerdictCalculator {
    private final String sessionId;
    private final ZonedDateTime time;
    private final SessionConfiguration sessionConfiguration;
    private final Map<String, KpiResult> kpiResults;
    private final MetricService metricService;

    public VerdictMessage createVerdict(final String fdn) {
        final var kpiResult = kpiResults.getOrDefault(fdn, new KpiResult());

        final var kpiVerdicts = sessionConfiguration.getKpiConfigs().stream()
                .filter(KpiConfiguration::isMonitor)
                .map(kpiConfig -> fixedVerdict(kpiResult, kpiConfig))
                .toList();

        final Map<VerdictEnum, Long> countsPerVerdictType = kpiVerdicts.stream()
                .collect(Collectors.groupingBy(KpiVerdict::getVerdict, Collectors.counting()));

        metricService.increment(PME_VERDICT_DEGRADED_COUNT, countsPerVerdictType.getOrDefault(VerdictEnum.DEGRADED, 0L));
        metricService.increment(PME_VERDICT_NOT_DEGRADED_COUNT, countsPerVerdictType.getOrDefault(VerdictEnum.NOT_DEGRADED, 0L));
        metricService.increment(PME_VERDICT_NOT_POSSIBLE_COUNT, countsPerVerdictType.getOrDefault(VerdictEnum.NOT_POSSIBLE, 0L));

        return VerdictMessage
                .newBuilder()
                .setPmeSessionId(sessionId)
                .setTimestamp(time.toInstant())
                .setFdn(fdn)
                .setKpiVerdicts(kpiVerdicts)
                .build();
    }

    private KpiVerdict fixedVerdict(final KpiResult kpiResult, final KpiConfiguration kpiConfiguration) {
        final var kpiName = kpiConfiguration.getKpiName();
        final var kpi = KPIS.get(kpiName);

        final var kpiValue = getValue(kpiResult, kpi.getKpiNames());
        final var threshold = kpiConfiguration.getFixedThresholdValue();
        final var verdictResult = fixedVerdictResult(kpiValue, threshold);

        return KpiVerdict.newBuilder()
                .setKpiName(kpiName)
                .setKpiValue(kpiValue)
                .setThresholdValue(threshold)
                .setVerdict(verdictResult)
                .setThresholdType(ThresholdTypeEnum.FIXED)
                .setKpiType(kpi.getType())
                .build();
    }

    private VerdictEnum fixedVerdictResult(final Double kpiValue, final Double threshold) {
        if (Objects.isNull(kpiValue)) {
            return VerdictEnum.NOT_POSSIBLE;
        }
        if (kpiValue < threshold) {
            return VerdictEnum.DEGRADED;
        }
        return VerdictEnum.NOT_DEGRADED;
    }

    private Double getValue(final KpiResult kpiResult, final List<String> keys) {
        return keys.stream()
                .map(kpiResult::get)
                .filter(Objects::nonNull)
                .map(Double.class::cast)
                .findFirst()
                .orElse(null);
    }
}
