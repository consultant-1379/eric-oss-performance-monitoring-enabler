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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.client.pmsc.KpiDefinitionApi;
import com.ericsson.oss.apps.client.pmsc.MonitorKpiCalculationsApi;
import com.ericsson.oss.apps.client.pmsqs.QueryKpiResultsApi;
import com.ericsson.oss.apps.config.GatewayProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PmschClientConfiguration {
    private final GatewayProperties gatewayProperties;

    @Bean
    @Primary
    public KpiDefinitionApi kpiDefinitionApi(@Qualifier("restTemplateConfig") final RestTemplate restTemplate) {
        final ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(gatewayProperties.getBasePath(PMSC));
        return new KpiDefinitionApi(apiClient);
    }

    @Bean
    @Primary
    public QueryKpiResultsApi queryKpiResultsApi(@Qualifier("restTemplateConfig") final RestTemplate restTemplate) {
        final ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(gatewayProperties.getBasePath(PMSQS));
        return new QueryKpiResultsApi(apiClient);
    }

    @Bean
    @Primary
    public MonitorKpiCalculationsApi monitorKpiCalculationsApi(@Qualifier("restTemplateConfig") final RestTemplate restTemplate) {
        final ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(gatewayProperties.getBasePath(PMSC));
        return new MonitorKpiCalculationsApi(apiClient);
    }
}
