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

package com.ericsson.oss.apps.config.client;

import static com.ericsson.oss.apps.util.Constants.PMSC;
import static com.ericsson.oss.apps.util.Constants.PMSQS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.client.pmsc.KpiDefinitionApi;
import com.ericsson.oss.apps.client.pmsqs.QueryKpiResultsApi;
import com.ericsson.oss.apps.config.GatewayProperties;

/**
 * Unit tests for {@link PmschClientConfiguration} class.
 */
class PmschClientConfigurationTest {
    private static final String PMSC_URL = "https://pmsc.test.ericsson.se";
    private static final String PMSC_BASE_PATH = "/kpi-handling";
    private static final String PMSC_API_CLIENT_BASE_PATH = PMSC_URL + PMSC_BASE_PATH;

    private final GatewayProperties gatewayProperties = new GatewayProperties();

    private final RestTemplate restTemplate = new RestTemplate();

    private PmschClientConfiguration objectUnderTest;

    @BeforeEach
    public void setUp() {
        final GatewayProperties.Service pmscService = new GatewayProperties.Service();

        pmscService.setUrl(PMSC_URL);
        pmscService.setBasePath(PMSC_BASE_PATH);
        gatewayProperties.setServices(Map.of(PMSC, pmscService, PMSQS, pmscService));
        objectUnderTest = new PmschClientConfiguration(gatewayProperties);
    }

    @Test
    void whenKpiDefinitionApiIsCreated_verifyBasePathOfApiClientConfiguredCorrectly() {
        final KpiDefinitionApi kpiDefinitionApi = objectUnderTest.kpiDefinitionApi(restTemplate);
        assertThat(kpiDefinitionApi.getApiClient().getBasePath()).isEqualTo(PMSC_API_CLIENT_BASE_PATH);
    }

    @Test
    void whenQueryKpiResultsApiIsCreated_verifyBasePathOfApiClientConfiguredCorrectly() {
        final QueryKpiResultsApi queryKpiResultsApi = objectUnderTest.queryKpiResultsApi(restTemplate);
        assertThat(queryKpiResultsApi.getApiClient().getBasePath()).isEqualTo(PMSC_API_CLIENT_BASE_PATH);
    }
}