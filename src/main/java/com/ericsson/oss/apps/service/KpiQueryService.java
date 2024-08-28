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

import static com.ericsson.oss.apps.util.Constants.JSON;
import static com.ericsson.oss.apps.util.Constants.KPI;
import static com.ericsson.oss.apps.util.Constants.ORDER_BY_FDN;
import static com.ericsson.oss.apps.util.Constants.PME_CELL_COMPLEX;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsqs.QueryKpiResultsApi;
import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.client.pmsqs.model.KpiResults;
import com.ericsson.oss.apps.exception.KpiQueryHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiQueryService {
    private final QueryKpiResultsApi queryKpiResultsApi;

    @Retryable(retryFor = { KpiQueryHandlingException.class },
            maxAttemptsExpression = "${queryKpis.retry.get.maxAttempts}",
            backoff = @Backoff(delayExpression = "${queryKpis.retry.get.backoff}"))
    public Map<String, KpiResult> queryKpis(final ZonedDateTime aggregationBeginTime, final long batchSize, final long offset) {
        final var filter = "aggregation_begin_time eq %s".formatted(aggregationBeginTime.toString());

        final KpiResults results;

        log.debug("Querying KPIs for '{}', batch size {}, offset: {}", filter, batchSize, offset);

        try {
            results = queryKpiResultsApi.exposureV1SchemaEntitysetGet(
                    KPI, PME_CELL_COMPLEX, null, ORDER_BY_FDN,
                    BigDecimal.valueOf(batchSize), BigDecimal.valueOf(offset), null, filter, JSON);
        } catch (final ResourceAccessException | HttpServerErrorException | TokenAuthenticationException e) {
            log.error("Failed to query KPI results", e);
            throw new KpiQueryHandlingException("Failed to query KPI results", e);
        }

        return results.getValue()
                .parallelStream()
                .collect(Collectors.toUnmodifiableMap(this::getFdn, Function.identity()));
    }

    public Map<String, KpiResult> filterKpis(final Map<String, KpiResult> kpis, final Collection<String> fdns) {
        return kpis.entrySet()
                .parallelStream()
                .filter(entry -> fdns.remove(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getFdn(final KpiResult kpiResult) {
        return (String) kpiResult.getOrDefault("fullFdn", null);
    }
}
