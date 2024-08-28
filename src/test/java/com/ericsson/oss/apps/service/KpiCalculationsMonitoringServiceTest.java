/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.oss.apps.client.pmsc.MonitorKpiCalculationsApi;
import com.ericsson.oss.apps.client.pmsc.model.CalculationGetResponseInner;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponse;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponseReadinessLogsInner;
import com.ericsson.oss.apps.exception.KpiCalculationMonitoringHandlingException;
import com.ericsson.oss.apps.exception.TokenAuthenticationException;


@EnableRetry
@SpringBootTest(properties = {
        "kpiCalculationMonitoring.retry.get.maxAttempts=3",
        "kpiCalculationMonitoring.retry.get.backoff=500",
        "kpiCalculationMonitoring.kpiReadinessRetry.numberOfRetries=3",
        "kpiCalculationMonitoring.kpiReadinessRetry.backoff=500"
}, classes = { KpiCalculationsMonitoringService.class })
public class KpiCalculationsMonitoringServiceTest {

    @Autowired
    private KpiCalculationsMonitoringService objectUnderTest;

    @MockBean
    private MonitorKpiCalculationsApi monitorKpiCalculationsApi;

    @Test
    void whenGetKpiCalculationsForElapsedTimeCalled_thenFindCalculationsCreatedAfterIsCalled() {
        final var calculationsResponse = calculationsGetResponse("test_id", "test_group", "test_kpi_type", "FINISHED");

        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        final var result = objectUnderTest.getKpiCalculationsForElapsedTime(120);

        assertThat(result)
                .hasSize(1)
                .contains(calculationsResponse);
        verify(monitorKpiCalculationsApi, times(1)).findCalculationsCreatedAfter(120, false);
    }

    @Test
    void whenCheckKpiReadinessIsCalled_andExceptionOccursInFindCalculationsCreatedAfter_thenRetries() {
        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false))
                .thenThrow(mock(ResourceAccessException.class))
                .thenThrow(mock(HttpServerErrorException.class))
                .thenThrow(mock(TokenAuthenticationException.class));

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        assertThatCode(() -> objectUnderTest.checkKpiReadiness(executionTime,false,120))
                .isInstanceOf(KpiCalculationMonitoringHandlingException.class);

        verify(monitorKpiCalculationsApi, times(3)).findCalculationsCreatedAfter(120, false);
    }

    @Test
    void whenGetKpiCalculationsByIdCalled_thenGetApplicationStateIsCalled() {
        final var readinessLog = readinessLog(3, "test_datasource", "2023-12-14T07:30:00",
                "2023-12-14T08:30:00");

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "simple",
                Collections.singletonList(readinessLog), "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);

        final var result = objectUnderTest.getKpiCalculationById("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6");

        assertThat(result)
                .isInstanceOf(CalculationResponse.class)
                .isEqualTo(calculationByIdResponse);

        verify(monitorKpiCalculationsApi, times(1)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
    }
    @Test
    void whenCheckKpiReadinessIsCalled_andExceptionOccursInGetApplicationState_thenRetries() {
        final var calculationsResponse = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "test_group", "SCHEDULED_COMPLEX", "FINISHED");

        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenThrow(mock(ResourceAccessException.class))
                .thenThrow(mock(HttpServerErrorException.class))
                .thenThrow(mock(TokenAuthenticationException.class));

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        assertThatCode(() -> objectUnderTest.checkKpiReadiness(executionTime,false,120))
                .isInstanceOf(KpiCalculationMonitoringHandlingException.class);

        verify(monitorKpiCalculationsApi, times(3)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
    }

    @Test
    void whenCheckKpiReadinessCalled_andRequiredNumberRopsNotFound_andRetryTrue_thenRetries_andRopListReturned() throws InterruptedException {
        final var calculationsResponse = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");
        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        final var readinessLog = readinessLog(3, "test_datasource", "2023-12-14T04:30:00",
                "2023-12-14T05:30:00");
        final var readinessLogValid = readinessLog(3, "test_datasource", "2023-12-14T07:30:00",
                "2023-12-14T07:30:00");

        final var readinessLogsList = Arrays.asList(readinessLog, readinessLogValid);

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group", readinessLogsList, "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        final var result = objectUnderTest.checkKpiReadiness(executionTime, true, 120);

        final var expectedRops = List.of(ZonedDateTime.parse("2023-12-14T07:30:00z"));

        assertThat(result)
                .hasSize(1)
                .isEqualTo(expectedRops);

        verify(monitorKpiCalculationsApi, times(3)).findCalculationsCreatedAfter(120, false);
        verify(monitorKpiCalculationsApi, times(3)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
    }


    @Test
    void whenCheckKpiReadinessCalled_andNoCalculatedRopFound_thenRetries_andEmptyRopListReturned() throws InterruptedException {
        final var calculationsResponse = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");
        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        final var readinessLog = readinessLog(3, "test_datasource", "2023-12-14T04:30:00",
                "2023-12-14T05:30:00");
        final var readinessLogValid = readinessLog(3, "test_datasource", "2023-12-14T05:45:00",
                "2023-12-14T05:45:00");

        final var readinessLogsList = Arrays.asList(readinessLog, readinessLogValid);

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group", readinessLogsList, "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        final var result = objectUnderTest.checkKpiReadiness(executionTime, true, 120);

        assertThat(result)
                .isEmpty();

        verify(monitorKpiCalculationsApi, times(3)).findCalculationsCreatedAfter(120, false);
        verify(monitorKpiCalculationsApi, times(3)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
    }

    @Test
    void whenCheckKpiReadinessCalled_andAllCorrectRopsAreFound_andRetryTrue_thenNoRetires_andCorrectRopListReturned() throws InterruptedException{
        final var calculationsResponseCorrect = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");
        final var calculationsResponseCorrectTwo = calculationsGetResponse("9a7f754d-41d7-415c-a6e5-1292cc03d4e9", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");
        final var calculationsResponseNotFinished = calculationsGetResponse("07f07868-2f26-44c3-acfb-ba7f94773852", "pme_complex_group",
                "SCHEDULED_COMPLEX", "IN_PROGRESS");
        final var calculationsResponseSimpleGroup = calculationsGetResponse("85464014-044a-4801-9d3f-901a55e8fd4c",
                "4G|PM_COUNTERS|EUtranCellFDD_1", "SCHEDULED_SIMPLE", "FINISHED");

        final List<CalculationGetResponseInner> calculationResponses = Arrays.asList(calculationsResponseCorrect, calculationsResponseCorrectTwo,
                calculationsResponseNotFinished,
                calculationsResponseSimpleGroup);

        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(calculationResponses);

        final var readinessLogRopOne = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                "2023-12-14T07:45:00", "2023-12-14T07:45:00");
        final var readinessLogRopTwo = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                "2023-12-14T07:30:00", "2023-12-14T07:30:00");
        final var readinessLogRopThreeAndFour = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                "2023-12-14T07:00:00", "2023-12-14T07:15:00");
        final var readinessLogRopOneSecond = readinessLog(3,
                "5G|PM_COUNTERS|NRCellCU_GNBCUCP_1", "2023-12-14T07:45:00", "2023-12-14T07:45:00");
        final var readinessLogRopNotInHour = readinessLog(3, "5G|PM_COUNTERS|NRCellCU_GNBCUCP_1",
                "2023-12-14T06:45:00", "2023-12-14T06:45:00");

        final var readinessLogsList = Arrays.asList(readinessLogRopThreeAndFour, readinessLogRopOneSecond,
                readinessLogRopNotInHour);

        final var readinessLogsSecondList = Arrays.asList(readinessLogRopOne,
                readinessLogRopTwo);

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                readinessLogsList, "FINISHED");

        final var calculationByIdSecondResponse = calculationByIdResponse("9a7f754d-41d7-415c-a6e5-1292cc03d4e9", "pme_complex_group",
                readinessLogsSecondList, "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);
        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("9a7f754d-41d7-415c-a6e5-1292cc03d4e9")))
                .thenReturn(calculationByIdSecondResponse);

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        final var result = objectUnderTest.checkKpiReadiness(executionTime, true, 120);

        final var expectedRops = Arrays.asList(ZonedDateTime.parse("2023-12-14T07:00:00z"), ZonedDateTime.parse("2023-12-14T07:15:00z"),
                ZonedDateTime.parse("2023-12-14T07:30:00z"), ZonedDateTime.parse("2023-12-14T07:45:00z"));

        assertThat(result)
                .hasSize(4)
                .isEqualTo(expectedRops);

        verify(monitorKpiCalculationsApi, times(1)).findCalculationsCreatedAfter(120, false);
        verify(monitorKpiCalculationsApi, times(1)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
        verify(monitorKpiCalculationsApi, times(1)).getApplicationState(UUID.fromString("9a7f754d-41d7-415c-a6e5-1292cc03d4e9"));

    }

    @Test
    void whenCheckKpiReadinessCalled_andRequiredNumberRopsNotFound_andRetryFalse_thenNoRetries_andRopListReturned() throws InterruptedException {
        final var calculationsResponse = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");
        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        final var readinessLog = readinessLog(3, "test_datasource", "2023-12-14T04:30:00",
                "2023-12-14T05:30:00");
        final var readinessLogValid = readinessLog(3, "test_datasource", "2023-12-14T07:30:00",
                "2023-12-14T07:30:00");

        final var readinessLogsList = Arrays.asList(readinessLog, readinessLogValid);

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group", readinessLogsList, "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);

        final ZonedDateTime executionTime = ZonedDateTime.parse("2023-12-14T07:00:00z");

        final var result = objectUnderTest.checkKpiReadiness(executionTime, false, 120);

        final var expectedRops = List.of(ZonedDateTime.parse("2023-12-14T07:30:00z"));

        assertThat(result)
                .hasSize(1)
                .isEqualTo(expectedRops);

        verify(monitorKpiCalculationsApi, times(1)).findCalculationsCreatedAfter(120, false);
        verify(monitorKpiCalculationsApi, times(1)).getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6"));
    }

    private CalculationGetResponseInner calculationsGetResponse(final String calculationId, final String executionGroup, final String kpiType,
            final String status) {
        final CalculationGetResponseInner calculationGetResponseInner = new CalculationGetResponseInner();
        calculationGetResponseInner.setCalculationId(calculationId);
        calculationGetResponseInner.setExecutionGroup(executionGroup);
        calculationGetResponseInner.setKpiType(kpiType);
        calculationGetResponseInner.setStatus(status);
        return calculationGetResponseInner;
    }

    private CalculationResponse calculationByIdResponse(final String calculationId, final String executionGroup,
            final List<CalculationResponseReadinessLogsInner> calculationResponseReadinessLogsInnerList, final String status) {
        final CalculationResponse calculationResponse = new CalculationResponse();
        calculationResponse.setCalculationId(calculationId);
        calculationResponse.setExecutionGroup(executionGroup);
        calculationResponseReadinessLogsInnerList
                .forEach(calculationResponse::addReadinessLogsItem);
        calculationResponse.setStatus(status);
        return calculationResponse;
    }

    private CalculationResponseReadinessLogsInner readinessLog(final int collectedRowCount, final String dataSource,
            final String earliestCollectedData, final String latestCollectedData) {
        final CalculationResponseReadinessLogsInner calculationResponseReadinessLogsInner = new CalculationResponseReadinessLogsInner();
        calculationResponseReadinessLogsInner.setCollectedRowCount(collectedRowCount);
        calculationResponseReadinessLogsInner.setDatasource(dataSource);
        calculationResponseReadinessLogsInner.setEarliestCollectedData(earliestCollectedData);
        calculationResponseReadinessLogsInner.setLatestCollectedData(latestCollectedData);
        return calculationResponseReadinessLogsInner;
    }

}
