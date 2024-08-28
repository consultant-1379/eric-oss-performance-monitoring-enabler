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

import static com.ericsson.oss.apps.util.Constants.MetricConstants.CODE;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CONFIGURATION_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.CONFIGURATION_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.DMM_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.METHOD;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ID_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SESSION_ID_STATUS_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.TOKEN_GATEWAY_ENDPOINT;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.URI;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class MetricServiceConfigTest {
    private static final String ENDPOINT_NAME = "my_endpoint";
    private static final String API_GATEWAY_ENDPOINT = "/auth/v1/login";
    private static final String KPI_DEFINITIONS_ENDPOINT = "/kpi-handling/model/v1/definitions";
    private static final String ACTUATOR_ENDPOINT = "/actuator";
    private static final Tag URI_TAG = Tag.of(URI, ENDPOINT_NAME);
    private static final Tag STATUS_TAG = Tag.of(STATUS, String.valueOf(HttpStatus.OK.value()));
    private static final Tag METHOD_TAG = Tag.of(METHOD, HttpMethod.GET.toString());
    private static final String HTTP_SERVER_REQUEST_METRIC_NAME = "http.server.requests";
    private static final String METRIC_NAME_STARTING_WITH_HTTP_SERVER_REQUEST = "http.server.requests.active";
    private static final String PME_HTTP_SERVER_REQUEST_METRIC_NAME = "pme.http.requests.duration";
    private static final String PME_ACTUATOR_HTTP_REQUESTS_DURATION = "pme.act.http.requests.duration";
    private static final String HTTP_CLIENT_REQUEST_METRIC_NAME = "http.client.requests";
    private static final String METRIC_NAME_STARTING_WITH_HTTP_CLIENT_REQUEST = "http.client.requests.active";
    private static final String PME_HTTP_CLIENT_REQUEST_METRIC_NAME = "pme.http.client.requests.duration";
    private static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION = "pme.apigateway.sessionid.http.requests.duration";
    private static final String DMM_HTTP_REQUESTS_DURATION = "pme.dmm.processing.http.requests.duration";
    private static final String KPI_HTTP_REQUESTS_DURATION = "pme.kpi.processing.http.requests.duration";
    private static final String PME_HTTP_REQUESTS_DURATION = "pme.http.requests.duration";
    private static final String TOKENGATEWAY_HTTP_REQUESTS_DURATION = "pme.tokengateway.http.requests.duration";
    private static final String ANOTHER_METRIC_NAME = "Another.Metric.Name";
    private static final String METRIC_DESC = "Some metric description";

    private MetricServiceConfig objectUnderTest;
    private Meter.Id meterId;

    @BeforeEach
    public void setup() {
        objectUnderTest = new MetricServiceConfig();
    }

    @Test
    void whenMeterIsHttpServerRequests_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(new Tag[] {}), null, METRIC_DESC, Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_SERVER_REQUEST_METRIC_NAME);
    }

    @Test
    void whenMeterIsNotHttpServerRequests_thenNameIsNotChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(METRIC_NAME_STARTING_WITH_HTTP_SERVER_REQUEST, Tags.of(new Tag[] {}), null, METRIC_DESC, Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(METRIC_NAME_STARTING_WITH_HTTP_SERVER_REQUEST);
    }

    @Test
    void whenMeterIsNotHttpClientRequests_thenNameIsNotChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(METRIC_NAME_STARTING_WITH_HTTP_CLIENT_REQUEST, Tags.of(new Tag[] {}), null, METRIC_DESC, Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(METRIC_NAME_STARTING_WITH_HTTP_CLIENT_REQUEST);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsActuator_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, ACTUATOR_ENDPOINT)), null, METRIC_DESC, Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_ACTUATOR_HTTP_REQUESTS_DURATION);
    }

    @Test
    void whenMeterIsHttpClientRequestsAndEndpointIsApiGateway_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, API_GATEWAY_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION);
    }

    @Test
    void whenMeterIsHttpClientRequestsAndEndpointIsDmm_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, DMM_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(DMM_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(DMM_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsConfiguration_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, CONFIGURATION_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(CONFIGURATION_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsConfigurationId_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, CONFIGURATION_ID_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(CONFIGURATION_ID_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsSession_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, SESSION_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(SESSION_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsSessionId_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, SESSION_ID_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(SESSION_ID_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpServerRequestsAndEndpointIsSessionIdStatus_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, SESSION_ID_STATUS_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(SESSION_ID_STATUS_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpClientRequestsAndEndpointIsTokenGateway_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, TOKEN_GATEWAY_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(TOKENGATEWAY_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(TOKEN_GATEWAY_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpClientRequestsAndEndpointIsKpi_thenNameIsChangedToRequiredNamingConventionAndKpiEndpointIsGeneralized() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(Tag.of(URI, KPI_DEFINITIONS_ENDPOINT)), null, METRIC_DESC,
                Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(KPI_HTTP_REQUESTS_DURATION);
        assertThat(objectUnderTest.meterFilter().map(meterId).getTag(ENDPOINT)).isEqualTo(KPI_DEFINITIONS_ENDPOINT);
    }

    @Test
    void whenMeterIsHttpClientRequests_thenNameIsChangedToRequiredNamingConvention() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(new Tag[] {}), null, METRIC_DESC, Meter.Type.COUNTER);
        assertThat(getMetricName()).isEqualTo(PME_HTTP_CLIENT_REQUEST_METRIC_NAME);
    }

    @Test
    void whenMeterIsHttpServerRequests_thenTagsForCodeAndMethodAndEndpointAreAdded() {
        meterId = new Meter.Id(HTTP_SERVER_REQUEST_METRIC_NAME, Tags.of(URI_TAG, STATUS_TAG, METHOD_TAG), null, METRIC_DESC, Meter.Type.COUNTER);
        verifyExpectedTags(objectUnderTest.meterFilter().map(meterId));
    }

    @Test
    void whenMeterIsHttpClientRequests_thenTagsForCodeAndMethodAndEndpointAreAdded() {
        meterId = new Meter.Id(HTTP_CLIENT_REQUEST_METRIC_NAME, Tags.of(URI_TAG, STATUS_TAG, METHOD_TAG), null, METRIC_DESC, Meter.Type.COUNTER);
        verifyExpectedTags(objectUnderTest.meterFilter().map(meterId));
    }

    @Test
    void whenMeterIsNotHttpRequests_thenNameAndTagsRemainUnchanged() {
        meterId = new Meter.Id(ANOTHER_METRIC_NAME, Tags.of(URI_TAG, STATUS_TAG, METHOD_TAG), null,
                METRIC_DESC, Meter.Type.COUNTER);
        final Meter.Id filteredMeterId = objectUnderTest.meterFilter().map(meterId);
        assertThat(filteredMeterId.getTags()).hasSize(3);
        assertThat(filteredMeterId.getTag(STATUS)).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(filteredMeterId.getTag(URI)).isEqualTo(ENDPOINT_NAME);
        assertThat(filteredMeterId.getTag(METHOD)).isEqualTo(HttpMethod.GET.toString());

        assertThat(filteredMeterId.getName()).isEqualTo(ANOTHER_METRIC_NAME);
    }

    private void verifyExpectedTags(final Meter.Id filteredMeterId) {
        assertThat(filteredMeterId).isNotNull();
        assertThat(filteredMeterId.getTag(CODE)).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(filteredMeterId.getTag(ENDPOINT)).isEqualTo(ENDPOINT_NAME);
        assertThat(filteredMeterId.getTag(METHOD)).isEqualTo(HttpMethod.GET.toString());
        assertThat(filteredMeterId.getTags()).hasSize(3);
    }

    private String getMetricName() {
        final Meter.Id id = objectUnderTest.meterFilter().map(meterId);
        assertThat(id).isNotNull();
        return id.getName();
    }
}
