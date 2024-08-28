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

package com.ericsson.oss.apps.e2e;

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
import static com.ericsson.oss.apps.util.Constants.JSON;
import static com.ericsson.oss.apps.util.Constants.KPI;
import static com.ericsson.oss.apps.util.Constants.MONITORING_OBJECT_TOPIC;
import static com.ericsson.oss.apps.util.Constants.NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.ORDER_BY_FDN;
import static com.ericsson.oss.apps.util.Constants.PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY;
import static com.ericsson.oss.apps.util.Constants.PME_CELL_COMPLEX;
import static com.ericsson.oss.apps.util.Constants.PME_SUBSCRIPTION_NAMES;
import static com.ericsson.oss.apps.util.Constants.SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB;
import static com.ericsson.oss.apps.util.Constants.SUBSCRIPTION_STATUS_ACTIVE;
import static com.ericsson.oss.apps.util.Constants.UL_PUSCH_SINR_HOURLY;
import static com.ericsson.oss.apps.util.Constants.VERDICT_TOPIC;
import static com.ericsson.oss.apps.util.Constants.VERSION;
import static com.ericsson.oss.apps.util.Constants.VOIP_CELL_INTEGRITY_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.KPI_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_DISCARDED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_NULL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_KPI_RETRIEVAL_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_MO_MONITORED_LOOKBACK_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_PA_EXECUTION_TIME_HOURLY;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_CREATED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_FINISHED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STARTED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_SESSION_STOPPED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_DEGRADED_COUNT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.PME_VERDICT_NOT_POSSIBLE_COUNT;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.FDN_FDD;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLCU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_NRCELLDU;
import static com.ericsson.oss.apps.util.TestConstants.FDN_TDD;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static com.ericsson.oss.apps.util.TestConstants.TEST_CONFIGURATION_NAME_ONE;
import static com.ericsson.oss.apps.util.TestConstants.TEST_CONFIGURATION_NAME_TWO;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.api.model.EpmeConfiguration;
import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.api.model.EpmeFixed;
import com.ericsson.oss.apps.api.model.EpmeSessionRequest;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.client.ddc.V1SubscriptionsApi;
import com.ericsson.oss.apps.client.ddc.model.SubscriptionGetAllRes;
import com.ericsson.oss.apps.client.pmsc.KpiDefinitionApi;
import com.ericsson.oss.apps.client.pmsc.MonitorKpiCalculationsApi;
import com.ericsson.oss.apps.client.pmsc.model.CalculationGetResponseInner;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponse;
import com.ericsson.oss.apps.client.pmsc.model.CalculationResponseReadinessLogsInner;
import com.ericsson.oss.apps.client.pmsqs.QueryKpiResultsApi;
import com.ericsson.oss.apps.client.pmsqs.model.KpiResults;
import com.ericsson.oss.apps.kafka.KafkaTestProducer;
import com.ericsson.oss.apps.kafka.VerdictKafkaTestConsumer;
import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.model.VerdictMessage;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionRepository;
import com.ericsson.oss.apps.scheduler.SchedulingJob;
import com.ericsson.oss.apps.service.StartupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EmbeddedKafka(partitions = 1, ports = 9093, topics = { "${spring.kafka.topics.monitoringObjectTopic}", "${spring.kafka.topics.verdictTopic}" })
@AutoConfigureObservability
@ActiveProfiles({ "test", "contract" })
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = { "startup.initialDelayInSeconds=2",
        "execution.schedule.cron-expression=*/15 * * * * *", "spring.kafka.bootstrap-server=localhost:9093" })
class EndToEndFlowTest {
    private static final ZonedDateTime AGGREGATION_BEGIN_TIME = ZonedDateTime.now().truncatedTo(HOURS).minusHours(1);
    private static final String SESSIONS = VERSION + "/sessions?clientId=%s";
    private static final String CONFIGURATIONS = VERSION + "/configurations";

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    @MockBean
    private V1SubscriptionsApi mockedSubscriptionApi;
    @MockBean
    private KpiDefinitionApi mockedKpiDefinitionApi;
    @Autowired
    private StartupService startupService;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${spring.kafka.topics.monitoringObjectTopic}")
    private String topic;
    @Autowired
    private KafkaTestProducer kafkaTestProducer;
    @Autowired
    private MonitoringObjectRepository monitoringObjectRepository;
    @MockBean
    private QueryKpiResultsApi queryKpiResultsApi;
    @MockBean
    private MonitorKpiCalculationsApi monitorKpiCalculationsApi;
    @SpyBean
    private SchedulingJob schedulingJob;

    @Autowired
    private VerdictKafkaTestConsumer verdictKafkaTestConsumer;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        monitoringObjectRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        monitoringObjectRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void validateEpmeE2EFlow() throws Exception {
        mockExternalServices();

        final EpmeConfigurationRequest epmeConfigurationRequest1 = createEpmeConfigurationRequest(TEST_CONFIGURATION_NAME_ONE);

        mvc.perform(post(CONFIGURATIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(epmeConfigurationRequest1)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.name").value(TEST_CONFIGURATION_NAME_ONE),
                        jsonPath("$.weekendDays").value(EpmeWeekendDays.SATURDAY_SUNDAY.toString()),
                        jsonPath("$.fixedThresholdKpis[*].kpiName", containsInAnyOrder(
                                ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB,
                                DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB,
                                SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB,
                                AVG_DL_MAC_DRB_THROUGHPUT,
                                AVG_UL_MAC_UE_THROUGHPUT,
                                NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY,
                                ENDC_PS_CELL_CHANGE_SUCCESS_RATE,
                                NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY,
                                NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY,
                                PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY,
                                CELL_AVAILABILITY_HOURLY,
                                INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY,
                                AVG_UL_PDCP_UE_THROUGHPUT_CELL,
                                AVG_DL_PDCP_UE_THROUGHPUT_CELL,
                                UL_PUSCH_SINR_HOURLY,
                                CELL_HANDOVER_SUCCESS_RATE_HOURLY,
                                E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY,
                                AVG_DL_LATENCY_HOURLY,
                                VOIP_CELL_INTEGRITY_HOURLY)));

        //Create second configuration to test update and deletion
        final EpmeConfigurationRequest epmeConfigurationRequest2 = createEpmeConfigurationRequest(TEST_CONFIGURATION_NAME_TWO);
        mvc.perform(post(CONFIGURATIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(epmeConfigurationRequest2)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.name").value(TEST_CONFIGURATION_NAME_TWO),
                        jsonPath("$.weekendDays").value(EpmeWeekendDays.SATURDAY_SUNDAY.toString()));

        final String json = mvc.perform(get(CONFIGURATIONS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final TypeFactory typeFactory = objectMapper.getTypeFactory();
        final List<EpmeConfiguration> configurations = new ObjectMapper().readValue(json,
                typeFactory.constructCollectionType(List.class, EpmeConfiguration.class));

        final String sessionConfigurationId = getConfigurationId(configurations, TEST_CONFIGURATION_NAME_ONE);
        assertThat(sessionConfigurationId).isNotNull();

        final var session = new EpmeSessionRequest(SESSION_REFERENCE, sessionConfigurationId);

        mvc.perform(post(String.format(SESSIONS, CLIENT_APP_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(session)))
                .andExpectAll(
                        status().isServiceUnavailable(),
                        jsonPath("$.title").value("Service is not ready"));

        awaitFor("Waiting for service to become ready.",
                () -> StartupService.StartupStatus.READY.equals(startupService.getStatus()));

        mvc.perform(
                post(String.format(SESSIONS, CLIENT_APP_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(session)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(PME_SESSION_ID),
                        jsonPath("$.sessionReference").value(SESSION_REFERENCE),
                        jsonPath("$.duration").value(18L),
                        jsonPath("$.pmeConfigId").value(sessionConfigurationId),
                        jsonPath("$.monitoringObjectTopicName").value(MONITORING_OBJECT_TOPIC),
                        jsonPath("$.verdictTopicName").value(VERDICT_TOPIC),
                        jsonPath("$.status").value("CREATED"),
                        jsonPath("$.createdAt").isNotEmpty());

        assertThat(sessionRepository.count())
                .isOne();
        assertThat(sessionRepository.existsByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE))
                .isTrue();

        final var startTime = AGGREGATION_BEGIN_TIME.minusMinutes(30).toInstant();

        final var fdns = List.of(FDN_FDD, FDN_TDD, FDN_NRCELLCU, FDN_NRCELLDU);
        fdns.forEach(fdn -> kafkaTestProducer.sendMessage(topic,
                new MonitoringObjectMessage(PME_SESSION_ID, fdn, startTime, StateEnum.ENABLED)));

        awaitFor("Waiting for Kafka message to be received.",
                () -> monitoringObjectRepository.findAllByPmeSessionId(PME_SESSION_ID).size() == 4);

        assertThat(monitoringObjectRepository.findAllByPmeSessionId(PME_SESSION_ID))
                .hasSize(4)
                .extracting(MonitoringObject::getFdn)
                .containsExactlyInAnyOrder(FDN_FDD, FDN_TDD, FDN_NRCELLCU, FDN_NRCELLDU);

        assertThat(sessionRepository.getReferenceById(PME_SESSION_ID))
                .isNotNull()
                .satisfies((persisted) -> {
                    assertThat(persisted.getStatus()).isEqualByComparingTo(SessionStatus.STARTED);
                    assertThat(persisted.getStartedAt()).isNotNull();
                });

        reset(schedulingJob);

        await("Scheduled execution to be called")
                .atMost(30, TimeUnit.SECONDS)
                .with()
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(schedulingJob, times(1)).executeScheduledMethod());

        awaitFor("Consume verdict messages", () -> verdictKafkaTestConsumer.getMessages().size() == 4);

        verifyVerdicts(verdictKafkaTestConsumer.getMessages());

        verify(monitorKpiCalculationsApi, times(1)).findCalculationsCreatedAfter(any(), any());

        verify(monitorKpiCalculationsApi, times(1)).getApplicationState(any());

        // validate read configurations
        assertThat(configurations).isNotEmpty();
        final String testConfigurationId = getConfigurationId(configurations, TEST_CONFIGURATION_NAME_TWO);
        assertThat(testConfigurationId).isNotNull();

        // validate update
        epmeConfigurationRequest2.setWeekendDays(EpmeWeekendDays.SUNDAY_MONDAY);
        mvc.perform(put(CONFIGURATIONS + "/" + testConfigurationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(epmeConfigurationRequest2)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.weekendDays").value(EpmeWeekendDays.SUNDAY_MONDAY.toString()));

        //validate delete configuration
        mvc.perform(delete(CONFIGURATIONS + "/" + testConfigurationId)).andExpect(status().isNoContent());

        // Verify the Metrics
        final String metricsOutput = mvc.perform(get("/actuator/prometheus").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        verifyMetrics(metricsOutput, KPI_HTTP_REQUESTS_DURATION_SECONDS, DMM_HTTP_REQUESTS_DURATION_SECONDS,
                APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS, PME_HTTP_REQUESTS_DURATION_SECONDS, PME_SESSION_CREATED_COUNT,
                PME_SESSION_STARTED_COUNT, PME_SESSION_FINISHED_COUNT, PME_SESSION_STOPPED_COUNT, PME_VERDICT_DEGRADED_COUNT,
                PME_VERDICT_NOT_DEGRADED_COUNT, PME_VERDICT_NOT_POSSIBLE_COUNT, PME_MO_KPI_RETRIEVAL_COUNT, PME_MO_KPI_NULL_COUNT,
                PME_SESSION_STARTED_COUNT, PME_SESSION_FINISHED_COUNT,
                PME_SESSION_STOPPED_COUNT, PME_MO_COUNT, PME_MO_DISCARDED_COUNT, PME_MO_MONITORED_COUNT, PME_MO_MONITORED_LOOKBACK_COUNT,
                PME_PA_EXECUTION_TIME_HOURLY);
    }

    private String getConfigurationId(final List<EpmeConfiguration> configurations, final String configurationName) {
        for (final EpmeConfiguration configuration : configurations) {
            if (configuration.getName().equals(configurationName)) {
                return configuration.getId();
            }
        }
        return null;
    }

    private void verifyVerdicts(final List<VerdictMessage> verdicts) {
        verdicts.sort(Comparator.comparing(verdict -> verdict.getFdn().toString()));
        verdicts.forEach(v -> v.getKpiVerdicts().sort(Comparator.comparing(kpi -> kpi.getKpiName().toString())));
        assertThat(verdicts).hasSize(4);
        assertThat(verdicts)
                .containsExactlyInAnyOrderElementsOf(loadExpectsVerdictsContent());
    }

    private void verifyMetrics(final String metricOutput, final String... metricNames) {
        final List<String> metrics = Arrays.stream(metricNames)
                .map(metricName -> metricName.replace(".", "_"))
                .collect(Collectors.toList());
        assertThat(metricOutput).contains(metrics);
    }

    private void mockExternalServices() {
        final var subscriptions = PME_SUBSCRIPTION_NAMES.stream().map(this::mockSubscription).toList();
        when(mockedSubscriptionApi.queryAllSubscriptionByParams(anyMap()))
                .thenReturn(subscriptions);

        when(mockedKpiDefinitionApi.addKpiDefinitionsWithHttpInfo(any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.CREATED));

        final var kpiResults = loadMockedResponseContent();

        when(queryKpiResultsApi.exposureV1SchemaEntitysetGet(eq(KPI), eq(PME_CELL_COMPLEX), eq(null), eq(ORDER_BY_FDN),
                eq(BigDecimal.valueOf(80_000L)), eq(BigDecimal.ZERO),
                eq(null), anyString(), eq(JSON)))
                        .thenReturn(kpiResults);

        final var calculationsResponse = calculationsGetResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                "SCHEDULED_COMPLEX", "FINISHED");

        when(monitorKpiCalculationsApi.findCalculationsCreatedAfter(120, false)).thenReturn(Collections.singletonList(calculationsResponse));

        final String ropOne = AGGREGATION_BEGIN_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final String ropTwo = AGGREGATION_BEGIN_TIME.plusMinutes(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final String ropThree = AGGREGATION_BEGIN_TIME.plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final String ropFour = AGGREGATION_BEGIN_TIME.plusMinutes(45).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        final var readinessLogRopOne = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                ropOne, ropOne);
        final var readinessLogRopTwo = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                ropTwo, ropTwo);
        final var readinessLogRopThree = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                ropThree, ropThree);
        final var readinessLogRopFour = readinessLog(3, "4G|PM_COUNTERS|EUtranCellFDD_1",
                ropFour, ropFour);

        final var calculationByIdResponse = calculationByIdResponse("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6", "pme_complex_group",
                Arrays.asList(readinessLogRopOne, readinessLogRopTwo, readinessLogRopThree, readinessLogRopFour), "FINISHED");

        when(monitorKpiCalculationsApi.getApplicationState(UUID.fromString("80ec5995-c56f-49aa-8a2a-0f3380c5d3f6")))
                .thenReturn(calculationByIdResponse);
    }

    private SubscriptionGetAllRes mockSubscription(final String name) {
        final var mock = mock(SubscriptionGetAllRes.class);
        when(mock.getStatus()).thenReturn(SUBSCRIPTION_STATUS_ACTIVE);
        when(mock.getName()).thenReturn(name);
        return mock;
    }

    private byte[] toJson(final Object object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    private void awaitFor(final String alias, final Callable<Boolean> until) {
        await(alias)
                .atMost(10, TimeUnit.SECONDS)
                .with()
                .pollInterval(1, TimeUnit.SECONDS)
                .until(until);
    }

    private KpiResults loadMockedResponseContent() {
        try {
            final String fileContent = Files.readString(
                    Path.of(ClassLoader.getSystemResource("e2eKpiComplexResponse.json").toURI()));

            return objectMapper.readValue(fileContent, KpiResults.class);
        } catch (final IOException | URISyntaxException e) {
            fail("TEST SETUP ERROR - Failed to read mocked KPI response", e);
        }
        return null;
    }

    private List<VerdictMessage> loadExpectsVerdictsContent() {
        try {
            final var content = Files.readString(Path.of(ClassLoader.getSystemResource("e2eFlowVerdicts.json").toURI()));
            final var messages = Arrays.asList(objectMapper.readValue(content, VerdictMessage[].class));

            // Set expected time for verdicts
            messages.forEach(verdict -> {
                verdict.setTimestamp(AGGREGATION_BEGIN_TIME.toInstant());
                verdict.getKpiVerdicts().sort(Comparator.comparing(kpi -> kpi.getKpiName().toString()));
            });
            return messages;
        } catch (final IOException | URISyntaxException e) {
            fail("TEST SETUP ERROR - Failed to read expected verdicts message", e);
        }

        return null;
    }

    private EpmeConfigurationRequest createEpmeConfigurationRequest(final String testConfigurationName) {
        final Set<EpmeFixed> epmeFixed = new HashSet<>();
        epmeFixed.add(new EpmeFixed(ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB, 95.0));
        epmeFixed.add(new EpmeFixed(DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB, 95.0));
        epmeFixed.add(new EpmeFixed(SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB, 95.0));
        epmeFixed.add(new EpmeFixed(AVG_DL_MAC_DRB_THROUGHPUT, 95000.0));
        epmeFixed.add(new EpmeFixed(AVG_UL_MAC_UE_THROUGHPUT, 95000.0));
        epmeFixed.add(new EpmeFixed(NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY, 95000.0));
        epmeFixed.add(new EpmeFixed(ENDC_PS_CELL_CHANGE_SUCCESS_RATE, 95.0));
        epmeFixed.add(new EpmeFixed(PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(CELL_AVAILABILITY_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(AVG_UL_PDCP_UE_THROUGHPUT_CELL, 95000.0));
        epmeFixed.add(new EpmeFixed(AVG_DL_PDCP_UE_THROUGHPUT_CELL, 95000.0));
        epmeFixed.add(new EpmeFixed(UL_PUSCH_SINR_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(CELL_HANDOVER_SUCCESS_RATE_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(AVG_DL_LATENCY_HOURLY, 95.0));
        epmeFixed.add(new EpmeFixed(VOIP_CELL_INTEGRITY_HOURLY, 95.99));
        return new EpmeConfigurationRequest(testConfigurationName, EpmeWeekendDays.SATURDAY_SUNDAY, epmeFixed);
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
