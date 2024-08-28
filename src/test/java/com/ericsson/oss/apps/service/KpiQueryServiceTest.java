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
import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLCU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_TDD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsqs.QueryKpiResultsApi;
import com.ericsson.oss.apps.client.pmsqs.model.KpiResult;
import com.ericsson.oss.apps.client.pmsqs.model.KpiResults;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;

@EnableRetry
@ActiveProfiles("test")
@DirtiesContext
@SpringBootTest(properties = { "queryKpis.retry.get.maxAttempts=3", "queryKpis.retry.get.backoff=500", }, classes = { KpiQueryService.class })
class KpiQueryServiceTest {
    private static final ZonedDateTime AGGREGATION_BEGIN_TIME = ZonedDateTime.of(2023, 11, 21, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String AGGREGATION_BEGIN_TIME_FILTER = "aggregation_begin_time eq %s".formatted(AGGREGATION_BEGIN_TIME.toString());
    private static final Long BATCH_SIZE = 10_000L;
    private static final BigDecimal BATCH_SIZE_BD = BigDecimal.valueOf(BATCH_SIZE);
    private static final Long OFFSET = 0L;
    private static final BigDecimal OFFSET_BD = BigDecimal.valueOf(OFFSET);

    @MockBean
    private QueryKpiResultsApi queryKpiResultsApi;

    @Autowired
    private KpiQueryService objectUnderTest;

    @Test
    void whenFetchKpis_thenQueryKpiResultsIsCalled() {
        final var fdn1 = kpiResult(FDN_FDD, 75.0D);
        final var fdn2 = kpiResult(FDN_TDD, 85.0D);
        final var fdn3 = kpiResult(FDN_NRCELLCU, 95.0D);

        when(queryKpiResultsApi.exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null, ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD,
                null, AGGREGATION_BEGIN_TIME_FILTER, JSON))
                        .thenReturn(new KpiResults(List.of(fdn1, fdn2, fdn3)));

        final var result = objectUnderTest.queryKpis(AGGREGATION_BEGIN_TIME, BATCH_SIZE, OFFSET);

        assertThat(result)
                .hasSize(3)
                .containsEntry(FDN_FDD, fdn1)
                .containsEntry(FDN_TDD, fdn2)
                .containsEntry(FDN_NRCELLCU, fdn3);

        verify(queryKpiResultsApi, times(1)).exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null,
                ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD, null, AGGREGATION_BEGIN_TIME_FILTER, JSON);
    }

    @Test
    void whenFetchKpis_andRuntimeExceptionOccurs_thenCheckActiveSubscriptionsIsNotRetried() {
        when(queryKpiResultsApi.exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null, ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD,
                null, AGGREGATION_BEGIN_TIME_FILTER, JSON))
                        .thenThrow(new RuntimeException());

        assertThatCode(() -> objectUnderTest.queryKpis(AGGREGATION_BEGIN_TIME, BATCH_SIZE, OFFSET))
                .isInstanceOf(RuntimeException.class);

        verify(queryKpiResultsApi, times(1)).exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null,
                ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD, null, AGGREGATION_BEGIN_TIME_FILTER, JSON);
    }

    @Test
    void whenFetchKpis_andExceptionOccursMaxAttempts_thenExceptionIsThrown() {
        when(queryKpiResultsApi.exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null, ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD,
                null, AGGREGATION_BEGIN_TIME_FILTER, JSON))
                        .thenThrow(mock(ResourceAccessException.class))
                        .thenThrow(mock(HttpServerErrorException.class))
                        .thenThrow(mock(TokenAuthenticationException.class));

        assertThatCode(() -> objectUnderTest.queryKpis(AGGREGATION_BEGIN_TIME, BATCH_SIZE, OFFSET))
                .isInstanceOf(RuntimeException.class);

        verify(queryKpiResultsApi, times(3)).exposureV1SchemaEntitysetGet(KPI, PME_CELL_COMPLEX, null,
                ORDER_BY_FDN, BATCH_SIZE_BD, OFFSET_BD, null, AGGREGATION_BEGIN_TIME_FILTER, JSON);
    }

    @Test
    void whenFilterKpis_verifyMapIsFilteredAndReturned() {
        final var fdn1 = kpiResult(FDN_FDD, 75.0D);
        final var fdn2 = kpiResult(FDN_TDD, 85.0D);
        final var fdn3 = kpiResult(FDN_NRCELLCU, 95.0D);

        final var kpis = Map.of(FDN_FDD, fdn1, FDN_TDD, fdn2, FDN_NRCELLCU, fdn3);
        final var filterKpis = new ArrayList<>(List.of(FDN_FDD, FDN_TDD));

        assertThat(objectUnderTest.filterKpis(kpis, filterKpis))
                .hasSize(2)
                .containsEntry(FDN_FDD, fdn1)
                .containsEntry(FDN_TDD, fdn2);

        assertThat(filterKpis).isEmpty();
    }

    @Test
    void whenFilterKpis_andNotAllFdnsIncluded_verifyMapIsFilteredAndReturnedAndFdnsSetIsNotEmpty() {
        final var fdn1 = kpiResult(FDN_FDD, 75.0D);
        final var fdn2 = kpiResult(FDN_TDD, 85.0D);

        final var kpis = Map.of(FDN_FDD, fdn1, FDN_TDD, fdn2);
        final var filterKpis = new ArrayList<>(List.of(FDN_FDD, FDN_TDD, FDN_NRCELLCU));

        assertThat(objectUnderTest.filterKpis(kpis, filterKpis))
                .hasSize(2)
                .containsEntry(FDN_FDD, fdn1)
                .containsEntry(FDN_TDD, fdn2);

        assertThat(filterKpis).hasSize(1).containsOnly(FDN_NRCELLCU);
    }

    private KpiResult kpiResult(final String fdn, final double cellHandoverSr) {
        final var kpiResult = new KpiResult();
        kpiResult.putAll(Map.of(
                "fullFdn", fdn,
                "aggregation_begin_time", AGGREGATION_BEGIN_TIME,
                "cell_handover_success_rate", cellHandoverSr));
        return kpiResult;
    }
}
