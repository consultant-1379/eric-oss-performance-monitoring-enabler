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

import static com.ericsson.oss.apps.util.Constants.IAM;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.client.iam.TokenEndpointApi;
import com.ericsson.oss.apps.config.GatewayProperties;

/**
 * Unit tests for {@link IamClientConfiguration} class.
 */
class IamClientConfigurationTest {
    private static final String IAM_URL = "https://iam.test.ericsson.se";
    private static final String IAM_BASE_PATH = "/auth/realms/master";
    private static final String TOKEN_ENDPOINT_PATH = "/protocol/openid-connect/token";
    private static final String IAM_API_CLIENT_BASE_PATH = IAM_URL + IAM_BASE_PATH;

    private final GatewayProperties gatewayProperties = new GatewayProperties();

    private final RestTemplate restTemplate = new RestTemplate();

    private IamClientConfiguration objectUnderTest;

    @BeforeEach
    public void setUp() {
        final GatewayProperties.Service iamService = new GatewayProperties.Service();

        iamService.setUrl(IAM_URL);
        iamService.setBasePath(IAM_BASE_PATH);
        gatewayProperties.setServices(new HashMap<>());
        gatewayProperties.getServices().put(IAM, iamService);

        objectUnderTest = new IamClientConfiguration(gatewayProperties);
    }

    @Test
    void whenTokenEndpointApiIsCreated_verifyBasePathOfApiClientConfiguredCorrectly() {
        final TokenEndpointApi tokenEndpointApi = objectUnderTest.tokenEndpointApi(restTemplate, TOKEN_ENDPOINT_PATH);

        assertThat(tokenEndpointApi.getApiClient().getBasePath())
                .isEqualTo(IAM_API_CLIENT_BASE_PATH);
    }
}